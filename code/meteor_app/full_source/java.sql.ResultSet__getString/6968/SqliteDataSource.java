package de.iweinzierl.passsafe.gui.data;

import java.io.File;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import de.iweinzierl.passsafe.shared.data.DataSourceChangedListener;
import de.iweinzierl.passsafe.shared.data.PassSafeDataSource;
import de.iweinzierl.passsafe.shared.data.SQLiteCommandExecutor;
import de.iweinzierl.passsafe.shared.data.SQLiteDatabaseCreator;
import de.iweinzierl.passsafe.shared.domain.DatabaseEntry;
import de.iweinzierl.passsafe.shared.domain.DatabaseEntryCategory;
import de.iweinzierl.passsafe.shared.domain.Entry;
import de.iweinzierl.passsafe.shared.domain.EntryCategory;
import de.iweinzierl.passsafe.shared.exception.PassSafeSqlException;
import de.iweinzierl.passsafe.shared.utils.DateUtils;

public class SqliteDataSource implements PassSafeDataSource {

    public static final String SQL_UPDATE_SYNCHRONIZATION_TIMESTAMP =
        "UPDATE passsafe_metadata SET value = ? WHERE meta_key = ?";

    public static final String SQL_QUERY_SYNCHRONIZATION_TIMESTAMP =
        "SELECT value FROM passsafe_metadata WHERE meta_key = ?";

    public static final String SQL_LOAD_CATEGORIES =
        "SELECT \"_id\", title, last_modified FROM category WHERE deleted = 0 ORDER BY title";

    public static final String SQL_LOAD_ENTRIES =
        "SELECT \"_id\", category_id, title, url, username, password, comment, last_modified FROM entry WHERE deleted = 0";

    public static final String SQL_INSERT_ENTRY =
        "INSERT INTO entry (category_id, title, url, username, password, comment, last_modified) VALUES (?, ?, ?, ?, ?, ?, ?)";

    public static final String SQL_REMOVE_ENTRY = "UPDATE entry SET deleted = 1 WHERE \"_id\" = ?";

    public static final String SQL_UPDATE_ENTRY =
        "UPDATE entry SET title = ?, url = ?, username = ?, password = ?, comment = ?, last_modified = ? WHERE \"_id\" = ?";

    public static final String SQL_FIND_ENTRY_ID = "SELECT \"_id\" FROM entry WHERE title = ?";

    public static final String SQL_INSERT_CATEGORY = "INSERT INTO category (title, last_modified) VALUES (?, ?)";

    public static final String SQL_REMOVE_CATEGORY = "UPDATE category SET deleted = 1 WHERE \"_id\" = ?";

    public static final String SQL_REMOVE_CATEGORY_ENTRIES = "DELETE FROM entry WHERE category_id = ?";

    public static final String SQL_UPDATE_CATEGORY_OF_ENTRY = "UPDATE entry SET category_id = ? WHERE \"_id\" = ?";

    private static final Logger LOGGER = LoggerFactory.getLogger(SqliteDataSource.class);

    private String dbfile;
    private DataSourceChangedListener dataSourceChangedListener;
    private Connection conn;

    private List<EntryCategory> categories;
    private Multimap<EntryCategory, Entry> entryMap;

    public SqliteDataSource(final String dbfile) throws SQLException, ClassNotFoundException, IOException,
        PassSafeSqlException {
        Class.forName("org.sqlite.JDBC");

        LOGGER.info("Establish database connection to {}", dbfile);

        this.dbfile = dbfile;
        this.categories = new ArrayList<>();
        this.entryMap = ArrayListMultimap.create();

        initialize(dbfile);
        preLoad();
    }

    private void initialize(final String dbfile) throws PassSafeSqlException, SQLException, ClassNotFoundException,
        IOException {

        File db = new File(dbfile);
        boolean isNew = !db.exists();

        conn = DriverManager.getConnection("jdbc:sqlite:" + dbfile);
        conn.setAutoCommit(true);

        if (isNew) {
            LOGGER.info("SQLite database is new. Initialize now...");

            try {
                new SQLiteDatabaseCreator(new SQLiteCommandExecutor() {
                        @Override
                        public boolean execute(final String sql) throws PassSafeSqlException {
                            try {
                                PreparedStatement preparedStatement = conn.prepareStatement(sql);
                                preparedStatement.execute();

                                return true;

                            } catch (SQLException e) {
                                throw new PassSafeSqlException(e.getMessage(), e.getCause());
                            }
                        }
                    }, SQLiteDatabaseCreator.OS.DESKTOP).setup();
            } catch (PassSafeSqlException e) {
                db.delete();
                throw e;
            }
        }
    }

    private void preLoad() throws SQLException {
        preLoadCategories();
        preLoadEntries();
    }

    private void preLoadCategories() throws SQLException {
        PreparedStatement loadCategories = conn.prepareStatement(SQL_LOAD_CATEGORIES);
        ResultSet resultSet = loadCategories.executeQuery();

        while (resultSet.next()) {
            int id = resultSet.getInt(1);
            String title = resultSet.getString(2);
            Date lastModified = DateUtils.parseDatabaseDate(resultSet.getString(3));

            categories.add(new DatabaseEntryCategory.Builder().withId(id).withTitle(title).withLastModified(
                    lastModified).build());
        }

        LOGGER.debug("PreLoaded {} categories", categories.size());
    }

    private void preLoadEntries() throws SQLException {
        PreparedStatement loadEntries = conn.prepareStatement(SQL_LOAD_ENTRIES);
        ResultSet resultSet = loadEntries.executeQuery();

        int loaded = 0;

        while (resultSet.next()) {
            int id = resultSet.getInt(1);
            int categoryId = resultSet.getInt(2);
            String title = resultSet.getString(3);
            String url = resultSet.getString(4);
            String username = resultSet.getString(5);
            String password = resultSet.getString(6);
            String comment = resultSet.getString(7);
            Date lastModified = DateUtils.parseDatabaseDate(resultSet.getString(8));

            DatabaseEntryCategory category = (DatabaseEntryCategory) getCategoryById(categoryId);

            if (category != null) {
                Collection<Entry> entries = entryMap.get(category);

                if (entries == null) {
                    entries = new ArrayList<>();
                    entryMap.put(category, (Entry) entries);
                }

                DatabaseEntry databaseEntry = new DatabaseEntry.Builder().withId(id).withCategory(category)
                                                                         .withTitle(title).withUrl(url)
                                                                         .withUsername(username).withPassword(password)
                                                                         .withComment(comment)
                                                                         .withLastModified(lastModified).build();
                entries.add(databaseEntry);
                loaded++;
            }
        }

        LOGGER.debug("PreLoaded {} entries", loaded);
    }

    private EntryCategory getCategoryById(final int id) {
        for (EntryCategory category : categories) {
            if (((DatabaseEntryCategory) category).getId() == id) {
                return category;
            }
        }

        return null;
    }

    @Override
    public void updateSynchronizationDate() {
        try {
            if (conn.isReadOnly()) {
                LOGGER.error("Database is read-only!");
                return;
            }

            PreparedStatement statement = conn.prepareStatement(SQL_UPDATE_SYNCHRONIZATION_TIMESTAMP);
            statement.setString(1, DateUtils.formatDatabaseDate(new Date()));
            statement.setString(2, METADATA_SYNCHRONIZATION_TIMESTAMP);

            int i = statement.executeUpdate();

            if (i <= 0) {
                LOGGER.error("Update of synchronization timestamp failed");
            }
        } catch (SQLException e) {
            LOGGER.error("Update of synchronization timestamp failed");
        }
    }

    @Override
    public Date getLastSynchronizationDate() {
        try {
            if (conn.isReadOnly()) {
                LOGGER.error("Database is read-only!");
                return null;
            }

            PreparedStatement statement = conn.prepareStatement(SQL_QUERY_SYNCHRONIZATION_TIMESTAMP);
            statement.setString(1, METADATA_SYNCHRONIZATION_TIMESTAMP);

            ResultSet result = statement.executeQuery();
            if (result != null) {
                String rawDate = result.getString(1);
                return DateUtils.parseDatabaseDate(rawDate);
            } else {
                LOGGER.error("Did not find a metadata row for '{}' to determine last synchronization timestamp",
                    METADATA_SYNCHRONIZATION_TIMESTAMP);
            }

        } catch (SQLException e) {
            LOGGER.error("Unable to determine synchronization timestamp");
        }

        return null;
    }

    @Override
    public int getItemCount(final EntryCategory category) {
        Collection<Entry> entries = entryMap.get(category);

        return entries == null || entries.isEmpty() ? 0 : entries.size();
    }

    @Override
    public List<EntryCategory> getCategories() {
        return categories;
    }

    @Override
    public List<Entry> getAllEntries(final EntryCategory category) {
        return (List<Entry>) entryMap.get(category);
    }

    @Override
    public Entry getEntry(final EntryCategory category, final int index) {
        List<Entry> entries = (List<Entry>) entryMap.get(category);
        if (entries != null && !entries.isEmpty() && entries.size() > index) {
            return entries.get(index);
        }

        return null;
    }

    @Override
    public Entry addEntry(final EntryCategory category, final Entry entry) {
        DatabaseEntryCategory sqliteCategory;

        if (!(category instanceof DatabaseEntryCategory)) {
            sqliteCategory = findCategory(category);
        } else {
            sqliteCategory = (DatabaseEntryCategory) category;
        }

        if (sqliteCategory == null) {
            LOGGER.error("Did not find sqlite category '{}'", category);
            return null;
        }

        try {
            if (conn.isReadOnly()) {
                LOGGER.error("Database is read-only!");
                return null;
            }

            Date now = new Date();

            PreparedStatement statement = conn.prepareStatement(SQL_INSERT_ENTRY);
            statement.setInt(1, sqliteCategory.getId());
            statement.setString(2, entry.getTitle());
            statement.setString(3, entry.getUrl());
            statement.setString(4, entry.getUsername());
            statement.setString(5, entry.getPassword());
            statement.setString(6, entry.getComment());
            statement.setString(7, DateUtils.formatDatabaseDate(now));

            statement.executeUpdate();

            int id = findId(entry);

            if (id <= 0) {
                LOGGER.error("Storage of entry '{}' was not successful", entry.toString());
                return null;
            }

            DatabaseEntry added = new DatabaseEntry.Builder().withEntry(entry).withId(id).build();
            added.setLastModified(now);

            entryMap.put(category, added);

            if (dataSourceChangedListener != null) {
                dataSourceChangedListener.onEntryAdded(category, added);
            }

            return added;
        } catch (SQLException e) {
            LOGGER.error("Unable to create new entry", e);
        }

        return null;
    }

    public Integer findId(final Entry entry) {
        try {
            PreparedStatement find = conn.prepareStatement(SQL_FIND_ENTRY_ID);
            find.setString(1, entry.getTitle());

            ResultSet resultSet = find.executeQuery();
            if (resultSet != null) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.error("Cannot find id for entry '{}'", entry, e);
        }

        return null;
    }

    public DatabaseEntryCategory findCategory(final EntryCategory category) {
        for (EntryCategory tmp : categories) {
            if (tmp.getTitle().equals(category.getTitle())) {
                return (DatabaseEntryCategory) category;
            }
        }

        return null;
    }

    @Override
    public void removeEntry(final Entry entry) {
        LOGGER.debug("Go an remove entry '{}'", entry);

        if (!(entry instanceof DatabaseEntry)) {
            LOGGER.warn("Cannot remove entry from type '{}'", entry.getClass());
            return;
        }

        try {
            PreparedStatement remove = conn.prepareStatement(SQL_REMOVE_ENTRY);
            remove.setInt(1, ((DatabaseEntry) entry).getId());

            int affected = remove.executeUpdate();

            if (affected <= 0) {
                LOGGER.error("Deletion of entry was not successful.");
                return;
            }

            entryMap.remove(entry.getCategory(), entry);
            LOGGER.info("Successfully deleted entry '{}'", entry);

        } catch (SQLException e) {
            LOGGER.error("Unable to remove entry '{}'", entry, e);
        }
    }

    @Override
    public void updateEntry(final Entry entry) {
        LOGGER.debug("Go and update entry '{}'", entry);

        if (!(entry instanceof DatabaseEntry)) {
            LOGGER.warn("Cannot remove entry from type '{}'", entry.getClass());
            return;
        }

        DatabaseEntry sqliteEntry = (DatabaseEntry) entry;

        try {
            PreparedStatement updateEntry = conn.prepareStatement(SQL_UPDATE_ENTRY);
            updateEntry.setString(1, entry.getTitle());
            updateEntry.setString(2, entry.getUrl());
            updateEntry.setString(3, entry.getUsername());
            updateEntry.setString(4, entry.getPassword());
            updateEntry.setString(5, entry.getComment());
            updateEntry.setString(6, DateUtils.formatDatabaseDate(new Date()));
            updateEntry.setInt(7, sqliteEntry.getId());

            int affected = updateEntry.executeUpdate();
            if (affected <= 0) {
                LOGGER.error("Update of entry was not successful.");
                return;
            }

            LOGGER.info("Successfully updated entry '{}'", entry);

        } catch (SQLException e) {
            LOGGER.error("Unable to update entry '{}'", entry, e);
        }
    }

    @Override
    public EntryCategory addCategory(final EntryCategory category) {
        LOGGER.debug("Go and insert category '{}'", category);

        try {
            PreparedStatement addCategory = conn.prepareStatement(SQL_INSERT_CATEGORY);
            addCategory.setString(1, category.getTitle());
            addCategory.setString(2, DateUtils.formatDatabaseDate(new Date()));
            addCategory.executeUpdate();

            ResultSet generatedKeys = addCategory.getGeneratedKeys();
            int id = generatedKeys.getInt(1);

            if (id > 0) {
                EntryCategory newCategory = new DatabaseEntryCategory.Builder().withId(id)
                                                                               .withTitle(category.getTitle()).build();
                LOGGER.info("Successfully inserted category '{}'", newCategory);

                categories.add(newCategory);

                return newCategory;
            }
        } catch (SQLException e) {
            // do nothing
        }

        LOGGER.error("Unable to add category '{}'", category);
        return null;
    }

    @Override
    public void removeCategory(final EntryCategory category) {
        LOGGER.debug("Go an remove category '{}'", category);

        if (!(category instanceof DatabaseEntryCategory)) {
            LOGGER.warn("Cannot remove category from type '{}'", category.getClass());
            return;
        }

        try {
            PreparedStatement removeEntries = conn.prepareStatement(SQL_REMOVE_CATEGORY_ENTRIES);
            removeEntries.setInt(1, ((DatabaseEntryCategory) category).getId());
            removeEntries.execute();

            PreparedStatement remove = conn.prepareStatement(SQL_REMOVE_CATEGORY);
            remove.setInt(1, ((DatabaseEntryCategory) category).getId());

            int affected = remove.executeUpdate();

            if (affected <= 0) {
                LOGGER.error("Deletion of category was not successful.");
                return;
            }

            categories.remove(category);
            LOGGER.info("Successfully deleted category '{}'", category);

        } catch (SQLException e) {
            LOGGER.error("Unable to remove category '{}'", category);
        }
    }

    @Override
    public void updateEntryCategory(final Entry entry, final EntryCategory category) {
        LOGGER.debug("Go and update category to '{}' of entry {}", category.getTitle(), entry.getTitle());

        final EntryCategory oldCategory = entry.getCategory();

        if (!(category instanceof DatabaseEntryCategory)) {
            LOGGER.warn("Cannot update entry with category from type '{}'", category.getClass());
            return;
        }

        if (!(entry instanceof DatabaseEntry)) {
            LOGGER.warn("Cannot update entry of type '{}'", entry.getClass());
            return;
        }

        try {
            PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_CATEGORY_OF_ENTRY);
            stmt.setInt(1, ((DatabaseEntryCategory) category).getId());
            stmt.setInt(2, ((DatabaseEntry) entry).getId());

            int affected = stmt.executeUpdate();
            if (affected <= 0) {
                LOGGER.warn("No entry updated!");
            } else {
                LOGGER.info("Successfully updated entry '{}' with category '{}'", entry.getTitle(),
                    category.getTitle());

                entryMap.remove(oldCategory, entry);
                entryMap.put(category, entry);
            }
        } catch (SQLException e) {
            LOGGER.error("Unable to update category '{}' of entry '{}'", category.getTitle(), entry.getTitle());
        }
    }

    @Override
    public void close() {
        if (conn != null) {

            try {

                LOGGER.info("Close connection to SQLite database '{}'", dbfile);
                conn.close();

            } catch (SQLException e) {
                LOGGER.error("Error while closing connection.", e);
            }
        }
    }

    @Override
    public void setDataSourceChangedListener(final DataSourceChangedListener listener) {
        this.dataSourceChangedListener = listener;
    }
}
