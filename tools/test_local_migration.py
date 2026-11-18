"""Vérifie la migration Room sur SQLite PC, sans accès à Android ou au téléphone."""
import json
from pathlib import Path
import re
import sqlite3
import unittest

PROJECT = Path(__file__).resolve().parent.parent
SCHEMAS = PROJECT / "app/schemas/fr.julie.mariosprint.data.local.SprintDatabase"
MIGRATION = PROJECT / "app/src/main/kotlin/fr/julie/mariosprint/data/local/DatabaseMigrations.kt"


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
        self.statements = re.findall(r'db\.execSQL\("""(.*?)"""\)', MIGRATION.read_text(encoding="utf-8"), re.S)
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


if __name__ == "__main__":
    unittest.main(verbosity=2)
