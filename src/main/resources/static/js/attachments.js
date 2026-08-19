// Admin-only "Εκκαθάριση Παλαιών Συνημμένων" panel: preview first (shows
// count + size without deleting anything), then confirm to actually run it.
// The run button is re-disabled whenever the days value changes, so a stale
// preview can never be executed against a different threshold.
document.addEventListener('DOMContentLoaded', function () {
    var cleanupDaysInput = document.getElementById('cleanup-days');
    var cleanupPreviewBtn = document.getElementById('cleanup-preview-btn');
    var cleanupRunBtn = document.getElementById('cleanup-run-btn');
    var cleanupResult = document.getElementById('cleanup-result');
    if (!cleanupPreviewBtn) {
        return;
    }

    var csrfToken = document.getElementById('csrf-token').value;

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
