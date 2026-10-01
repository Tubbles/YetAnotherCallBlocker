package dummydomain.yetanothercallblocker.data.db;

import android.content.Context;
import android.database.Cursor;

import org.greenrobot.greendao.database.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import dummydomain.yetanothercallblocker.data.BlacklistUtils;

public class YacbDbOpenHelper extends DaoMaster.OpenHelper {

    private static final Logger LOG = LoggerFactory.getLogger(YacbDbOpenHelper.class);

    public YacbDbOpenHelper(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        LOG.info("onUpgrade() oldVersion={}, newVersion={}", oldVersion, newVersion);

        if (oldVersion < 2) {
            // Every blacklist entry becomes a block rule in creation order,
            // and its LIKE pattern becomes the equivalent regular expression.
            addRuleColumns(db);
            fillPositions(db);
            convertLegacyPatterns(db);
        }

        LOG.info("onUpgrade() finished");
    }

    private static void addRuleColumns(Database db) {
        db.execSQL("ALTER TABLE \"" + BlacklistItemDao.TABLENAME + "\" ADD COLUMN \""
                + BlacklistItemDao.Properties.Allow.columnName
                + "\" INTEGER NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE \"" + BlacklistItemDao.TABLENAME + "\" ADD COLUMN \""
                + BlacklistItemDao.Properties.Position.columnName
                + "\" INTEGER NOT NULL DEFAULT 0");
    }

    private static void fillPositions(Database db) {
        String table = BlacklistItemDao.TABLENAME;
        String positionColumn = BlacklistItemDao.Properties.Position.columnName;
        String creationDateColumn = BlacklistItemDao.Properties.CreationDate.columnName;

        db.execSQL("UPDATE \"" + table + "\" SET \"" + positionColumn + "\" = (SELECT COUNT(*)"
                + " FROM \"" + table + "\" AS older"
                + " WHERE older.\"" + creationDateColumn + "\""
                + " < \"" + table + "\".\"" + creationDateColumn + "\")");
    }

    private static void convertLegacyPatterns(Database db) {
        String table = BlacklistItemDao.TABLENAME;
        String idColumn = BlacklistItemDao.Properties.Id.columnName;
        String patternColumn = BlacklistItemDao.Properties.Pattern.columnName;
        String invalidColumn = BlacklistItemDao.Properties.Invalid.columnName;

        List<Long> ids = new ArrayList<>();
        List<String> patterns = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT \"" + idColumn + "\", \"" + patternColumn + "\""
                + " FROM \"" + table + "\"", null);
        try {
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
                patterns.add(cursor.getString(1));
            }
        } finally {
            cursor.close();
        }

        String updateSql = "UPDATE \"" + table + "\""
                + " SET \"" + patternColumn + "\" = ?, \"" + invalidColumn + "\" = ?"
                + " WHERE \"" + idColumn + "\" = ?";

        int converted = 0;
        for (int index = 0; index < ids.size(); index++) {
            String legacyPattern = patterns.get(index);
            if (legacyPattern == null) continue;

            String pattern = BlacklistUtils.legacyPatternToRegex(legacyPattern);

            db.execSQL(updateSql, new Object[]{pattern,
                    BlacklistUtils.isValidPattern(pattern) ? 0 : 1, ids.get(index)});
            converted++;
        }

        LOG.info("convertLegacyPatterns() converted {} pattern(s)", converted);
    }

}
