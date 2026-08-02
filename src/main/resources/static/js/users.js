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
    var menu = document.getElementById('user-modal-menu');

    var csrfToken = document.getElementById('csrf-token').value;

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
