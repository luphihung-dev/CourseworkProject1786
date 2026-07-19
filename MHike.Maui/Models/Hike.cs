using SQLite;

namespace MHike.Maui.Models;

/// <summary>
/// A planned hike stored in the local SQLite database.
/// Mirrors the schema used by the native Android version of M-Hike.
/// </summary>
[Table("hikes")]
public class Hike
{
    [PrimaryKey, AutoIncrement]
    [Column("id")]
    public int Id { get; set; }

    [Column("name")]
    public string Name { get; set; } = string.Empty;

    [Column("location")]
    public string Location { get; set; } = string.Empty;

    [Column("hike_date")]
    public DateTime Date { get; set; } = DateTime.Today;

    [Column("parking_available")]
    public bool ParkingAvailable { get; set; }

    [Column("length_km")]
    public double LengthKm { get; set; }

    [Column("difficulty")]
    public string Difficulty { get; set; } = string.Empty;

    [Column("description")]
    public string Description { get; set; } = string.Empty;

    // Custom fields required by the specification
    [Column("duration_hours")]
    public double EstimatedDurationHours { get; set; }

    [Column("terrain_type")]
    public string TerrainType { get; set; } = string.Empty;

    /// <summary>Summary line shown underneath the hike name in the list.</summary>
    [Ignore]
    public string Summary => $"{Location}  ·  {Date:ddd, dd MMM yyyy}  ·  {LengthKm} km";
}
