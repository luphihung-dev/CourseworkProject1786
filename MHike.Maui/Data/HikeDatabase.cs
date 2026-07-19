using MHike.Maui.Models;
using SQLite;

namespace MHike.Maui.Data;

/// <summary>
/// Data access layer for hikes. Opens the SQLite database lazily so the
/// connection is only created the first time it is needed.
/// </summary>
public class HikeDatabase
{
    private SQLiteAsyncConnection? _connection;

    private async Task<SQLiteAsyncConnection> GetConnectionAsync()
    {
        if (_connection is null)
        {
            string databasePath = Path.Combine(FileSystem.AppDataDirectory, "mhike.db3");
            _connection = new SQLiteAsyncConnection(databasePath,
                SQLiteOpenFlags.ReadWrite | SQLiteOpenFlags.Create | SQLiteOpenFlags.SharedCache);
            await _connection.CreateTableAsync<Hike>();
        }
        return _connection;
    }

    public async Task<List<Hike>> GetAllAsync()
    {
        var connection = await GetConnectionAsync();
        return await connection.Table<Hike>().OrderBy(h => h.Date).ToListAsync();
    }

    /// <summary>Inserts a new hike or updates an existing one based on its id.</summary>
    public async Task SaveAsync(Hike hike)
    {
        var connection = await GetConnectionAsync();
        if (hike.Id == 0)
        {
            await connection.InsertAsync(hike);
        }
        else
        {
            await connection.UpdateAsync(hike);
        }
    }

    public async Task DeleteAsync(Hike hike)
    {
        var connection = await GetConnectionAsync();
        await connection.DeleteAsync(hike);
    }

    /// <summary>Removes every hike from the database (the "reset" option).</summary>
    public async Task DeleteAllAsync()
    {
        var connection = await GetConnectionAsync();
        await connection.DeleteAllAsync<Hike>();
    }
}
