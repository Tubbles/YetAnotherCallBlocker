package dummydomain.yetanothercallblocker.data.db;

import org.greenrobot.greendao.query.CloseableListIterator;
import org.greenrobot.greendao.query.Query;
import org.greenrobot.greendao.query.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import dummydomain.yetanothercallblocker.BlacklistDataSource;

public class BlacklistDao {

    public interface DaoSessionProvider {
        DaoSession getDaoSession();
    }

    private static final Logger LOG = LoggerFactory.getLogger(BlacklistDao.class);

    private final DaoSessionProvider daoSessionProvider;

    public BlacklistDao(DaoSessionProvider daoSessionProvider) {
        this.daoSessionProvider = daoSessionProvider;
    }

    public BlacklistDataSource.Factory dataSourceFactory() {
        return new BlacklistDataSource.Factory(this);
    }

    public List<BlacklistItem> loadAll() {
        return getDefaultQueryBuilder().list();
    }

    public QueryBuilder<BlacklistItem> getDefaultQueryBuilder() {
        return getBlacklistItemDao().queryBuilder()
                .orderAsc(BlacklistItemDao.Properties.Position)
                .orderAsc(BlacklistItemDao.Properties.CreationDate);
    }

    public <T extends Collection<BlacklistItem>> T detach(T items) {
        BlacklistItemDao dao = getBlacklistItemDao();
        for (BlacklistItem item : items) {
            dao.detach(item);
        }
        return items;
    }

    public BlacklistItem findById(long id) {
        return getBlacklistItemDao().load(id);
    }

    public BlacklistItem findByPattern(String pattern) {
        return first(getBlacklistItemDao().queryBuilder()
                .where(BlacklistItemDao.Properties.Pattern.eq(pattern))
                .orderAsc(BlacklistItemDao.Properties.Pattern));
    }

    public BlacklistItem findByNameAndPattern(String name, String pattern) {
        return first(getBlacklistItemDao().queryBuilder()
                .where(BlacklistItemDao.Properties.Name.eq(name))
                .where(BlacklistItemDao.Properties.Pattern.eq(pattern))
                .orderAsc(BlacklistItemDao.Properties.Pattern));
    }

    public void save(BlacklistItem blacklistItem) {
        getBlacklistItemDao().save(blacklistItem);
    }

    public void insert(BlacklistItem blacklistItem) {
        getBlacklistItemDao().insert(blacklistItem);
    }

    public void delete(Iterable<Long> keys) {
        getBlacklistItemDao().deleteByKeyInTx(keys);
    }

    public long countValid() {
        return getBlacklistItemDao().queryBuilder()
                .where(BlacklistItemDao.Properties.Invalid.notEq(true)).count();
    }

    /**
     * Regular expressions cannot be evaluated by SQLite, so the rules are matched in memory.
     * The list is small (a few dozen rules at most) and it is loaded once per screened call.
     *
     * @return all usable rules in the order they are evaluated in
     */
    public List<BlacklistItem> loadValidInOrder() {
        return getBlacklistItemDao().queryBuilder()
                .where(BlacklistItemDao.Properties.Invalid.notEq(true))
                .orderAsc(BlacklistItemDao.Properties.Position)
                .orderAsc(BlacklistItemDao.Properties.CreationDate)
                .list();
    }

    public int getNextPosition() {
        BlacklistItem lastItem = first(getBlacklistItemDao().queryBuilder()
                .orderDesc(BlacklistItemDao.Properties.Position));

        return lastItem != null ? lastItem.getPosition() + 1 : 0;
    }

    public void swapPositions(BlacklistItem firstItem, BlacklistItem secondItem) {
        int firstPosition = firstItem.getPosition();
        firstItem.setPosition(secondItem.getPosition());
        secondItem.setPosition(firstPosition);

        getBlacklistItemDao().updateInTx(firstItem, secondItem);
    }

    private <T> T first(QueryBuilder<T> queryBuilder) {
        return first(queryBuilder.build());
    }

    private <T> T first(Query<T> query) {
        try (CloseableListIterator<T> it = query.listIterator()) {
            if (it.hasNext()) return it.next();
        } catch (IOException e) {
            LOG.debug("first()", e);
        }
        return null;
    }

    private BlacklistItemDao getBlacklistItemDao() {
        return daoSessionProvider.getDaoSession().getBlacklistItemDao();
    }

}
