using System.Globalization;
using System.Text;

using MHike.Maui.Data;
using MHike.Maui.Models;

namespace MHike.Maui;

/// <summary>
/// Entry form for adding or editing a hike (feature e). All required
/// fields are validated and the user confirms a summary of the details
/// before anything is saved.
/// </summary>
public partial class HikeFormPage : ContentPage
{
    private readonly HikeDatabase _database;
    private readonly Hike? _hikeBeingEdited;

    public HikeFormPage(HikeDatabase database, Hike? hikeToEdit = null)
    {
        InitializeComponent();
        _database = database;
        _hikeBeingEdited = hikeToEdit;

        if (hikeToEdit is not null)
        {
            Title = "Edit hike";
            PopulateForm(hikeToEdit);
        }
    }

    private void PopulateForm(Hike hike)
    {
        NameEntry.Text = hike.Name;
        LocationEntry.Text = hike.Location;
        HikeDatePicker.Date = hike.Date;
        ParkingYesRadio.IsChecked = hike.ParkingAvailable;
        ParkingNoRadio.IsChecked = !hike.ParkingAvailable;
        LengthEntry.Text = hike.LengthKm.ToString(CultureInfo.InvariantCulture);
        DifficultyPicker.SelectedItem = hike.Difficulty;
        DurationEntry.Text = hike.EstimatedDurationHours > 0
            ? hike.EstimatedDurationHours.ToString(CultureInfo.InvariantCulture)
            : string.Empty;
        TerrainPicker.SelectedItem = string.IsNullOrEmpty(hike.TerrainType) ? null : hike.TerrainType;
        DescriptionEditor.Text = hike.Description;
    }

    /// <summary>
    /// Additional feature: reads the device's position once and
    /// reverse-geocodes it into a readable place name for the location field.
    /// </summary>
    private async void OnUseCurrentLocationClicked(object? sender, EventArgs e)
    {
        try
        {
            var permission = await Permissions.RequestAsync<Permissions.LocationWhenInUse>();
            if (permission != PermissionStatus.Granted)
            {
                await DisplayAlert("Permission needed",
                    "Location permission is needed to fill this in automatically.", "OK");
                return;
            }

            var location = await Geolocation.GetLocationAsync(new GeolocationRequest(
                GeolocationAccuracy.Medium, TimeSpan.FromSeconds(15)));
            if (location is null)
            {
                await DisplayAlert("Location unavailable",
                    "Could not get your location. Check that location is turned on.", "OK");
                return;
            }

            var placemarks = await Geocoding.GetPlacemarksAsync(location.Latitude, location.Longitude);
            var place = placemarks?.FirstOrDefault();
            LocationEntry.Text = place is null
                ? $"{location.Latitude:F5}, {location.Longitude:F5}"
                : string.Join(", ", new[] { place.Locality ?? place.SubAdminArea, place.CountryName }
                    .Where(part => !string.IsNullOrEmpty(part)));
        }
        catch (Exception)
        {
            await DisplayAlert("Location unavailable",
                "Could not get your location. Check that location is turned on.", "OK");
        }
    }

    private async void OnSaveClicked(object? sender, EventArgs e)
    {
        Hike? hike = ValidateForm();
        if (hike is null)
        {
            return;
        }

        // Show all the details back to the user before saving, as required.
        bool confirmed = await DisplayAlert("Confirm hike details",
            BuildSummary(hike), "Confirm & save", "Go back & edit");
        if (!confirmed)
        {
            return;
        }

        await _database.SaveAsync(hike);
        await DisplayAlert("Saved", "The hike has been saved.", "OK");
        await Navigation.PopAsync();
    }

    /// <summary>
    /// Checks every required field, showing an inline error under each one
    /// that is missing or invalid.
    /// </summary>
    private Hike? ValidateForm()
    {
        bool valid = true;

        string name = NameEntry.Text?.Trim() ?? string.Empty;
        NameError.IsVisible = name.Length == 0;
        valid &= name.Length > 0;

        string location = LocationEntry.Text?.Trim() ?? string.Empty;
        LocationError.IsVisible = location.Length == 0;
        valid &= location.Length > 0;

        bool parkingChosen = ParkingYesRadio.IsChecked || ParkingNoRadio.IsChecked;
        ParkingError.IsVisible = !parkingChosen;
        valid &= parkingChosen;

        bool lengthValid = double.TryParse(LengthEntry.Text, NumberStyles.Float,
            CultureInfo.InvariantCulture, out double lengthKm) && lengthKm > 0;
        LengthError.IsVisible = !lengthValid;
        valid &= lengthValid;

        bool difficultyChosen = DifficultyPicker.SelectedItem is not null;
        DifficultyError.IsVisible = !difficultyChosen;
        valid &= difficultyChosen;

        // Optional field: only validated when the user typed something.
        double durationHours = 0;
        string durationText = DurationEntry.Text?.Trim() ?? string.Empty;
        if (durationText.Length > 0)
        {
            bool durationValid = double.TryParse(durationText, NumberStyles.Float,
                CultureInfo.InvariantCulture, out durationHours) && durationHours > 0;
            DurationError.IsVisible = !durationValid;
            valid &= durationValid;
        }
        else
        {
            DurationError.IsVisible = false;
        }

        if (!valid)
        {
            return null;
        }

        Hike hike = _hikeBeingEdited ?? new Hike();
        hike.Name = name;
        hike.Location = location;
        hike.Date = HikeDatePicker.Date;
        hike.ParkingAvailable = ParkingYesRadio.IsChecked;
        hike.LengthKm = lengthKm;
        hike.Difficulty = (string)DifficultyPicker.SelectedItem!;
        hike.EstimatedDurationHours = durationHours;
        hike.TerrainType = SelectedTextOrEmpty(TerrainPicker.SelectedItem);
        hike.Description = DescriptionEditor.Text?.Trim() ?? string.Empty;
        return hike;
    }

    private static string SelectedTextOrEmpty(object? pickerSelection)
    {
        return pickerSelection as string ?? string.Empty;
    }

    private string BuildSummary(Hike hike)
    {
        var summary = new StringBuilder();
        summary.AppendLine($"Name: {hike.Name}");
        summary.AppendLine($"Location: {hike.Location}");
        summary.AppendLine($"Date: {hike.Date:ddd, dd MMM yyyy}");
        summary.AppendLine($"Parking: {(hike.ParkingAvailable ? "Yes" : "No")}");
        summary.AppendLine($"Length: {hike.LengthKm} km");
        summary.AppendLine($"Difficulty: {hike.Difficulty}");
        summary.AppendLine($"Est. duration: {(hike.EstimatedDurationHours > 0 ? $"{hike.EstimatedDurationHours} hours" : "Not provided")}");
        summary.AppendLine($"Terrain: {(hike.TerrainType.Length > 0 ? hike.TerrainType : "Not provided")}");
        summary.Append($"Description: {(hike.Description.Length > 0 ? hike.Description : "Not provided")}");
        return summary.ToString();
    }
}
