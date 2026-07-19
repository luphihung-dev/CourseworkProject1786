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
