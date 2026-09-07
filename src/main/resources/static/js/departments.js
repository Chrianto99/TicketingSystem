// Client-side search filter for the Departments page — matches on name or location.
document.addEventListener('DOMContentLoaded', function () {
    var searchInput = document.getElementById('department-search');
    var list = document.getElementById('department-list');
    if (!searchInput || !list) {
        return;
    }

    var cards = Array.prototype.slice.call(list.querySelectorAll('.entity-card'));
    var divider = list.querySelector('.entity-list-divider');
    var emptyMessage = document.getElementById('department-search-empty');

    searchInput.addEventListener('input', function () {
        var query = searchInput.value.trim().toLowerCase();
        var visibleCount = 0;

        cards.forEach(function (card) {
            var name = card.querySelector('.entity-name .entity-edit-view');
            var location = card.querySelector('.entity-location');
            var text = ((name ? name.textContent : '') + ' ' + (location ? location.textContent : '')).toLowerCase();
            var matches = !query || text.indexOf(query) !== -1;
            card.hidden = !matches;
            if (matches) {
                visibleCount++;
            }
        });

        if (divider) {
            var inactiveVisible = cards.some(function (card) {
                return !card.hidden && card.classList.contains('entity-card-inactive');
            });
            divider.hidden = !inactiveVisible;
        }

        if (emptyMessage) {
            emptyMessage.hidden = !(query && visibleCount === 0);
        }
    });
});
