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
    var specializationEl = document.getElementById('user-modal-specialization');
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
            openUserModal({
                id: card.getAttribute('data-user-id'),
                username: card.getAttribute('data-username'),
                role: card.getAttribute('data-role'),
                active: card.getAttribute('data-active') === 'true',
                scheduledDeletionAt: card.getAttribute('data-scheduled-deletion'),
                firstName: card.getAttribute('data-first-name'),
                lastName: card.getAttribute('data-last-name'),
                email: card.getAttribute('data-email'),
                phoneNumber: card.getAttribute('data-phone'),
                specialization: card.getAttribute('data-specialization')
            });
        });
    });

    var openUserParam = new URLSearchParams(window.location.search).get('openUser');
    if (openUserParam) {
        fetch('/api/users/' + openUserParam)
            .then(function (res) { return res.ok ? res.json() : Promise.reject(); })
            .then(function (user) { openUserModal(user); })
            .catch(function () {});
        var cleanUrl = window.location.pathname + window.location.hash;
        window.history.replaceState(null, '', cleanUrl);
    }

    function openUserModal(user) {
        var userId = user.id;
        var active = user.active;
        var scheduledDeletion = user.scheduledDeletionAt;

        titleEl.textContent = user.username;
        roleBadge.textContent = ROLE_LABELS[user.role] || user.role;
        roleBadge.className = 'badge role-' + user.role;

        var fullName = ((user.firstName || '') + ' ' + (user.lastName || '')).trim();
        if (fullName) {
            nameRow.hidden = false;
            nameEl.textContent = fullName;
        } else {
            nameRow.hidden = true;
        }

        emailEl.textContent = user.email || '—';
        phoneEl.textContent = user.phoneNumber || '—';
        specializationEl.textContent = user.specialization || '—';
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
            var isAdmin = user.role === 'ADMIN';
            var isSelf = currentUserId !== null && String(userId) === currentUserId;
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

    // Admin-only "Εκκαθάριση Παλαιών Συνημμένων" panel: preview first (shows
    // count + size without deleting anything), then confirm to actually run it.
    // The run button is re-disabled whenever the days value changes, so a stale
    // preview can never be executed against a different threshold.
    var cleanupDaysInput = document.getElementById('cleanup-days');
    var cleanupPreviewBtn = document.getElementById('cleanup-preview-btn');
    var cleanupRunBtn = document.getElementById('cleanup-run-btn');
    var cleanupResult = document.getElementById('cleanup-result');

    if (cleanupPreviewBtn) {
        cleanupDaysInput.addEventListener('input', function () {
            cleanupRunBtn.disabled = true;
        });

        cleanupPreviewBtn.addEventListener('click', function () {
            var days = cleanupDaysInput.value;
            cleanupResult.textContent = 'Φόρτωση…';
            fetchJson('/api/attachments/cleanup/preview?olderThanDays=' + encodeURIComponent(days))
                .then(function (data) {
                    if (data.count === 0) {
                        cleanupResult.textContent = 'Δεν βρέθηκαν συνημμένα προς εκκαθάριση.';
                        cleanupRunBtn.disabled = true;
                    } else {
                        cleanupResult.textContent = data.count + ' αρχεία (' + formatBytes(data.totalBytes) + ') θα διαγραφούν μόνιμα.';
                        cleanupRunBtn.disabled = false;
                    }
                })
                .catch(function (err) {
                    cleanupResult.textContent = '';
                    cleanupRunBtn.disabled = true;
                    window.showError('Αποτυχία προεπισκόπησης: ' + err.message);
                });
        });

        cleanupRunBtn.addEventListener('click', function () {
            var days = cleanupDaysInput.value;
            window.showConfirm('Οριστική διαγραφή αυτών των συνημμένων αρχείων; Αυτή η ενέργεια δεν μπορεί να αναιρεθεί.').then(function (ok) {
                if (!ok) {
                    return;
                }
                cleanupRunBtn.disabled = true;
                fetchJson('/api/attachments/cleanup?olderThanDays=' + encodeURIComponent(days), {
                    method: 'POST',
                    headers: { 'X-CSRF-TOKEN': csrfToken }
                })
                    .then(function (data) {
                        cleanupResult.textContent = 'Διαγράφηκαν ' + data.count + ' αρχεία (' + formatBytes(data.totalBytes) + ').';
                    })
                    .catch(function (err) {
                        cleanupRunBtn.disabled = false;
                        window.showError('Αποτυχία εκκαθάρισης: ' + err.message);
                    });
            });
        });
    }

    function fetchJson(url, options) {
        return fetch(url, options).then(function (res) {
            if (!res.ok) {
                return res.json().catch(function () {
                    return {};
                }).then(function (body) {
                    throw new Error(body.error || Object.values(body)[0] || ('HTTP ' + res.status));
                });
            }
            return res.json();
        });
    }

    function formatBytes(bytes) {
        if (bytes < 1024) {
            return bytes + ' B';
        }
        if (bytes < 1024 * 1024) {
            return (bytes / 1024).toFixed(1) + ' KB';
        }
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }
});
