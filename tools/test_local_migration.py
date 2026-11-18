"""Vérifie la migration Room sur SQLite PC, sans accès à Android ou au téléphone."""
import json
from pathlib import Path
import re
import sqlite3
import unittest

PROJECT = Path(__file__).resolve().parent.parent
SCHEMAS = PROJECT / "app/schemas/fr.julie.mariosprint.data.local.SprintDatabase"
MIGRATION = PROJECT / "app/src/main/kotlin/fr/julie/mariosprint/data/local/DatabaseMigrations.kt"


def statements_for(start, end):
    source = MIGRATION.read_text(encoding="utf-8")
    block = source.split(f"val FROM_{start}_TO_{end}", 1)[1].split("val FROM_", 1)[0]
    return re.findall(r'db\.execSQL\("""(.*?)"""\)', block, re.S)


class RoomMigrationTest(unittest.TestCase):
    def setUp(self):
        self.database = sqlite3.connect(":memory:")
        schema = json.loads((SCHEMAS / "1.json").read_text(encoding="utf-8"))["database"]
        for entity in schema["entities"]:
            self.database.execute(entity["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
        self.database.execute("INSERT INTO personal_stats VALUES ('prairie-1-v2',42000,5,2,17)")
        self.database.execute("INSERT INTO personal_stats VALUES ('prairie-1-v1',29829,7,1,38)")
        self.database.execute("INSERT INTO pending_records VALUES ('prairie-1-v2','alice',42000,12)")
        self.database.execute("INSERT INTO pending_records VALUES ('night-3-v1','bob',43000,9)")
        self.database.execute("INSERT INTO settings VALUES (1,0)")
        self.database.execute("INSERT INTO leaderboard_cache VALUES ('prairie-1-v2','alice','bob','Bob',43000,9)")
        self.original = {name: self.database.execute(f'SELECT * FROM "{name}"').fetchall()
                         for name in ("personal_stats", "pending_records", "settings", "leaderboard_cache")}
        self.statements = statements_for(1, 2)
        self.assertTrue(self.statements, "Aucune instruction de migration trouvée")
        for statement in self.statements:
            self.database.execute(statement)

    def tearDown(self):
        self.database.close()

    def test_existing_records_pending_accounts_settings_and_cache_are_preserved(self):
        for name, rows in self.original.items():
            self.assertEqual(rows, self.database.execute(f'SELECT * FROM "{name}"').fetchall(), name)

    def test_migrated_columns_types_and_keys_match_exported_room_schema(self):
        schema = json.loads((SCHEMAS / "2.json").read_text(encoding="utf-8"))["database"]
        self.assertEqual(2, schema["version"])
        self.assertEqual({entity["tableName"] for entity in schema["entities"]},
                         {row[0] for row in self.database.execute("SELECT name FROM sqlite_master WHERE type='table'")})
        for entity in schema["entities"]:
            primary_key = entity["primaryKey"]["columnNames"]
            expected = [(field["columnName"], field["affinity"], int(field.get("notNull", False)),
                         primary_key.index(field["columnName"]) + 1 if field["columnName"] in primary_key else 0)
                        for field in entity["fields"]]
            actual = [(row[1], row[2], row[3], row[5])
                      for row in self.database.execute(f'PRAGMA table_info("{entity["tableName"]}")')]
            self.assertEqual(expected, actual, entity["tableName"])

    def test_completed_levels_and_appearance_are_isolated_between_owners(self):
        for owner, level in (("guest", "night-3-v1"), ("account:alice", "prairie-1-v2"), ("account:bob", "canyon-2-v1")):
            self.database.execute("INSERT INTO completed_levels VALUES (?,?)", (owner, level))
            self.database.execute("INSERT INTO player_appearance VALUES (?,?,?,?)", (owner, "luigi", 0, 0))
        self.database.execute("UPDATE player_appearance SET skinId='toad', revision=1, pending=1 WHERE owner='account:alice'")
        self.assertEqual([("prairie-1-v2",)], self.database.execute("SELECT levelId FROM completed_levels WHERE owner='account:alice'").fetchall())
        self.assertEqual(("luigi", 0), self.database.execute("SELECT skinId, pending FROM player_appearance WHERE owner='account:bob'").fetchone())
        self.assertEqual(("luigi", 0), self.database.execute("SELECT skinId, pending FROM player_appearance WHERE owner='guest'").fetchone())
        with self.assertRaises(sqlite3.IntegrityError):
            self.database.execute("INSERT INTO completed_levels VALUES ('account:alice','prairie-1-v2')")

    def test_replayed_ddl_keeps_existing_and_new_progress(self):
        self.database.execute("INSERT INTO completed_levels VALUES ('account:alice','prairie-1-v2')")
        self.database.execute("INSERT INTO player_appearance VALUES ('account:alice','toad',1,1)")
        for statement in self.statements:
            self.database.execute(statement)
        self.test_existing_records_pending_accounts_settings_and_cache_are_preserved()
        self.assertEqual(("toad", 1), self.database.execute("SELECT skinId, pending FROM player_appearance WHERE owner='account:alice'").fetchone())


class FriendsMigrationTest(unittest.TestCase):
    def setUp(self):
        self.database = sqlite3.connect(":memory:")
        self.database.execute("PRAGMA foreign_keys=ON")
        schema = json.loads((SCHEMAS / "2.json").read_text(encoding="utf-8"))["database"]
        for entity in schema["entities"]:
            self.database.execute(entity["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
        self.database.execute("INSERT INTO personal_stats VALUES ('prairie-1-v1',29829,7,1,38)")
        self.database.execute("INSERT INTO pending_records VALUES ('night-3-v1','alice',43000,9)")
        self.database.execute("INSERT INTO settings VALUES (1,0)")
        self.database.execute("INSERT INTO leaderboard_cache VALUES ('prairie-1-v2','alice','bob','Bob',43000,9)")
        self.database.execute("INSERT INTO completed_levels VALUES ('account:alice','prairie-1-v2')")
        self.database.execute("INSERT INTO player_appearance VALUES ('account:alice','toad',3,1)")
        self.original = {entity["tableName"]: self.database.execute(f'SELECT * FROM "{entity["tableName"]}"').fetchall()
                         for entity in schema["entities"]}
        self.statements = statements_for(2, 3)
        self.assertEqual(4, len(self.statements))
        for statement in self.statements:
            self.database.execute(statement)

    def tearDown(self):
        self.database.close()

    def test_six_historical_tables_and_all_their_values_are_preserved(self):
        for table, rows in self.original.items():
            self.assertEqual(rows, self.database.execute(f'SELECT * FROM "{table}"').fetchall())

    def test_version_three_columns_primary_and_foreign_keys_match_exported_schema(self):
        schema = json.loads((SCHEMAS / "3.json").read_text(encoding="utf-8"))["database"]
        self.assertEqual(3, schema["version"])
        self.assertEqual({e["tableName"] for e in schema["entities"]},
                         {r[0] for r in self.database.execute("SELECT name FROM sqlite_master WHERE type='table'")})
        for entity in schema["entities"]:
            keys = entity["primaryKey"]["columnNames"]
            expected = [(f["columnName"], f["affinity"], int(f.get("notNull", False)),
                         keys.index(f["columnName"]) + 1 if f["columnName"] in keys else 0) for f in entity["fields"]]
            actual = [(r[1], r[2], r[3], r[5]) for r in self.database.execute(f'PRAGMA table_info("{entity["tableName"]}")')]
            self.assertEqual(expected, actual, entity["tableName"])
            expected_fks = sorted((fk["table"], src, target, fk["onUpdate"], fk["onDelete"])
                                  for fk in entity.get("foreignKeys", [])
                                  for src, target in zip(fk["columns"], fk["referencedColumns"]))
            actual_fks = sorted((r[2], r[3], r[4], r[5], r[6]) for r in self.database.execute(f'PRAGMA foreign_key_list("{entity["tableName"]}")'))
            self.assertEqual(expected_fks, actual_fks, entity["tableName"])

    def test_social_rows_are_isolated_by_viewer_and_profile(self):
        for viewer in ("alice", "bob"):
            self.database.execute("INSERT INTO friends_cache VALUES (?,'ABCDEF123456',1,100)", (viewer,))
            self.database.execute("INSERT INTO friend_relations_cache VALUES (?,'peer','r','Peer','luigi','incoming')", (viewer,))
            self.database.execute("INSERT INTO social_profiles_cache VALUES (?,'peer','Peer','luigi',NULL,100)", (viewer,))
            self.database.execute("INSERT INTO social_profile_records VALUES (?,'peer','prairie-1-v2',42000)", (viewer,))
        self.database.execute("DELETE FROM friend_relations_cache WHERE viewerUid='alice'")
        self.assertEqual(1, self.database.execute("SELECT count(*) FROM friend_relations_cache WHERE viewerUid='bob'").fetchone()[0])
        self.database.execute("DELETE FROM social_profiles_cache WHERE viewerUid='alice'")
        self.assertEqual([("bob",)], self.database.execute("SELECT viewerUid FROM social_profile_records").fetchall())
        with self.assertRaises(sqlite3.IntegrityError):
            self.database.execute("INSERT INTO friend_relations_cache VALUES ('unknown','p','r','P','luigi','friend')")

    def test_profile_cache_is_bounded_per_viewer_and_old_records_are_removed_in_cascade(self):
        for viewer in ("alice", "bob"):
            for number in range(60):
                peer = f"peer-{number:02}"
                self.database.execute("INSERT INTO social_profiles_cache VALUES (?,?,'Peer','luigi',NULL,?)", (viewer, peer, number))
                self.database.execute("INSERT INTO social_profile_records VALUES (?,?,'prairie-1-v2',42000)", (viewer, peer))
        source = (PROJECT / "app/src/main/kotlin/fr/julie/mariosprint/data/local/dao/FriendsDao.kt").read_text(encoding="utf-8")
        trim = re.search(r'@Query\("""(DELETE FROM social_profiles_cache.*?)"""\)', source, re.S).group(1)
        self.database.execute(trim, {"uid": "alice"})
        self.assertEqual(50, self.database.execute("SELECT count(*) FROM social_profiles_cache WHERE viewerUid='alice'").fetchone()[0])
        self.assertEqual(60, self.database.execute("SELECT count(*) FROM social_profiles_cache WHERE viewerUid='bob'").fetchone()[0])
        self.assertEqual(50, self.database.execute("SELECT count(*) FROM social_profile_records WHERE viewerUid='alice'").fetchone()[0])

    def test_failed_snapshot_transaction_keeps_the_previous_cache(self):
        self.database.execute("INSERT INTO friends_cache VALUES ('alice','ABCDEF123456',5,100)")
        self.database.execute("INSERT INTO friend_relations_cache VALUES ('alice','bob','r','Bob','luigi','incoming')")
        self.database.execute("SAVEPOINT cache_update")
        try:
            self.database.execute("DELETE FROM friend_relations_cache WHERE viewerUid='alice'")
            self.database.execute("UPDATE friends_cache SET revision=6 WHERE viewerUid='alice'")
            self.database.execute("INSERT INTO friend_relations_cache VALUES ('missing','peer','r','P','luigi','friend')")
        except sqlite3.IntegrityError:
            self.database.execute("ROLLBACK TO cache_update")
        self.assertEqual((5,), self.database.execute("SELECT revision FROM friends_cache WHERE viewerUid='alice'").fetchone())
        self.assertEqual(("bob",), self.database.execute("SELECT peerUid FROM friend_relations_cache WHERE viewerUid='alice'").fetchone())

    def test_replaying_additive_ddl_keeps_both_social_and_historical_data(self):
        self.database.execute("INSERT INTO friends_cache VALUES ('alice','ABCDEF123456',5,100)")
        for statement in self.statements:
            self.database.execute(statement)
        self.test_six_historical_tables_and_all_their_values_are_preserved()
        self.assertEqual((5,), self.database.execute("SELECT revision FROM friends_cache WHERE viewerUid='alice'").fetchone())


if __name__ == "__main__":
    unittest.main(verbosity=2)
