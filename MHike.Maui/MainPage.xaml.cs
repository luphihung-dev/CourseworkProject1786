using MHike.Maui.Data;
using MHike.Maui.Models;

namespace MHike.Maui;

/// <summary>
/// Home screen: lists all stored hikes and offers edit, delete and
/// reset-database actions (feature f).
/// </summary>
public partial class MainPage : ContentPage
{
    private readonly HikeDatabase _database;

    public MainPage(HikeDatabase database)
    {
        InitializeComponent();
        _database = database;
    }

    protected override async void OnAppearing()
    {
        base.OnAppearing();
        await LoadHikesAsync();
    }

    private async Task LoadHikesAsync()
    {
        List<Hike> hikes = await _database.GetAllAsync();
        HikeCollectionView.ItemsSource = hikes;
        bool isEmpty = hikes.Count == 0;
        EmptyStateLayout.IsVisible = isEmpty;
        HikeCollectionView.IsVisible = !isEmpty;
    }

    private async void OnAddHikeClicked(object? sender, EventArgs e)
    {
        await Navigation.PushAsync(new HikeFormPage(_database));
    }

    private async void OnEditHikeClicked(object? sender, EventArgs e)
    {
        if (sender is Button { CommandParameter: Hike hike })
        {
            await Navigation.PushAsync(new HikeFormPage(_database, hike));
        }
    }

    /// <summary>
    /// Additional feature: shares the hike details as plain text through
    /// any app the user picks (messaging, email, notes...).
    /// </summary>
    private async void OnShareHikeClicked(object? sender, EventArgs e)
    {
        if (sender is Button { CommandParameter: Hike hike })
        {
            string shareText = $"M-Hike – {hike.Name}\n"
                + $"Location: {hike.Location}\n"
                + $"Date: {hike.Date:ddd, dd MMM yyyy}\n"
                + $"Length: {hike.LengthKm} km\n"
                + $"Difficulty: {hike.Difficulty}";

            await Share.Default.RequestAsync(new ShareTextRequest
            {
                Title = "Share hike",
                Text = shareText
            });
        }
    }

    private async void OnDeleteHikeClicked(object? sender, EventArgs e)
    {
        if (sender is Button { CommandParameter: Hike hike })
        {
            bool confirmed = await DisplayAlert("Delete this hike?",
                $"\"{hike.Name}\" will be removed. This cannot be undone.",
                "Delete", "Cancel");
            if (confirmed)
            {
                await _database.DeleteAsync(hike);
                await LoadHikesAsync();
            }
        }
    }

    private async void OnDeleteAllClicked(object? sender, EventArgs e)
    {
        bool confirmed = await DisplayAlert("Delete all hikes?",
            "This removes every hike stored on this device and cannot be undone.",
            "Delete all", "Cancel");
        if (confirmed)
        {
            await _database.DeleteAllAsync();
            await LoadHikesAsync();
        }
    }
}
