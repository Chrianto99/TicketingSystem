// Users page: clicking a user card opens a modal with the full user record and,
// for admins, a "Reset Password" action plus a hidden options menu holding
// Delete (deactivates the user; actual deletion happens 7 days later) or,
// for an already-deactivated user, Reactivate instead.
document.addEventListener('DOMContentLoaded', function () {
    var modal = document.getElementById('user-modal');
    if (!modal) {
        return;
    }

    var ROLE_LABELS = { USER: 'Χρήστης', ADMIN: 'Διαχειριστής' };

    var titleEl = document.getElementById('user-modal-title');
    var roleBadge = document.getElementById('user-modal-role-badge');
    var nameRow = document.getElementById('user-modal-name-row');
    var nameEl = document.getElementById('user-modal-name');
    var emailEl = document.getElementById('user-modal-email');
    var phoneEl = document.getElementById('user-modal-phone');
    var statusEl = document.getElementById('user-modal-status');
    var deletionRow = document.getElementById('user-modal-deletion-row');
    var deletionEl = document.getElementById('user-modal-deletion');
    var resetForm = document.getElementById('user-modal-reset-form');
    var resetCsrf = document.getElementById('user-modal-reset-csrf');
    var deleteForm = document.getElementById('user-modal-delete-form');
    var deleteCsrf = document.getElementById('user-modal-delete-csrf');
    var reactivateForm = document.getElementById('user-modal-reactivate-form');
    var reactivateCsrf = document.getElementById('user-modal-reactivate-csrf');
    var toggleAdminForm = document.getElementById('user-modal-toggle-admin-form');
    var toggleAdminCsrf = document.getElementById('user-modal-toggle-admin-csrf');
    var toggleAdminBtn = document.getElementById('user-modal-toggle-admin-btn');
    var menu = document.getElementById('user-modal-menu');

    var csrfToken = document.getElementById('csrf-token').value;
    var currentUserId = document.body.getAttribute('data-current-user-id');

    document.querySelectorAll('.entity-card[data-user-id]').forEach(function (card) {
        card.addEventListener('click', function () {
            openUserModal(card);
        });
    });

    function openUserModal(card) {
        var userId = card.getAttribute('data-user-id');
        var username = card.getAttribute('data-username');
        var active = card.getAttribute('data-active') === 'true';
        var scheduledDeletion = card.getAttribute('data-scheduled-deletion');

        titleEl.textContent = username;
        roleBadge.textContent = ROLE_LABELS[card.getAttribute('data-role')] || card.getAttribute('data-role');
        roleBadge.className = 'badge role-' + card.getAttribute('data-role');

        var fullName = ((card.getAttribute('data-first-name') || '') + ' ' + (card.getAttribute('data-last-name') || '')).trim();
        if (fullName) {
            nameRow.hidden = false;
            nameEl.textContent = fullName;
        } else {
            nameRow.hidden = true;
        }

        emailEl.textContent = card.getAttribute('data-email') || '—';
        phoneEl.textContent = card.getAttribute('data-phone') || '—';
        statusEl.textContent = active ? 'Ενεργός' : 'Απενεργοποιημένος';

        if (!active && scheduledDeletion) {
            deletionRow.hidden = false;
            deletionEl.textContent = formatDate(scheduledDeletion);
        } else {
            deletionRow.hidden = true;
        }

        if (menu) {
            menu.removeAttribute('open');
        }

        if (resetForm) {
            resetForm.setAttribute('action', '/users/' + userId + '/reset-password');
            resetCsrf.value = csrfToken;
        }
        if (deleteForm) {
            deleteForm.setAttribute('action', '/users/' + userId + '/delete');
            deleteCsrf.value = csrfToken;
            deleteForm.hidden = !active;
        }
        if (reactivateForm) {
            reactivateForm.setAttribute('action', '/users/' + userId + '/reactivate');
            reactivateCsrf.value = csrfToken;
            reactivateForm.hidden = active;
        }
        if (toggleAdminForm) {
            var isAdmin = card.getAttribute('data-role') === 'ADMIN';
            var isSelf = currentUserId !== null && userId === currentUserId;
            toggleAdminForm.setAttribute('action', '/users/' + userId + '/toggle-admin');
            toggleAdminCsrf.value = csrfToken;
            toggleAdminForm.hidden = isSelf;
            toggleAdminBtn.textContent = isAdmin ? 'Αφαίρεση Δικαιωμάτων Διαχειριστή' : 'Ορισμός ως Διαχειριστής';
            toggleAdminForm.setAttribute('data-confirm', isAdmin
                ? 'Αφαίρεση δικαιωμάτων διαχειριστή από αυτόν τον χρήστη;'
                : 'Ορισμός αυτού του χρήστη ως διαχειριστή;');
        }

        modal.showModal();
    }

    function formatDate(isoString) {
        var d = new Date(isoString);
        if (isNaN(d.getTime())) {
            return isoString;
        }
        return d.toLocaleString();
    }
});
