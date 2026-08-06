// Some browsers (notably Firefox) restore this page from bfcache on refresh/back
// instead of re-requesting it, which would show stale data and a stuck edit-mode
// state. Force a real reload whenever that happens.
window.addEventListener('pageshow', function (event) {
    if (event.persisted) {
        window.location.reload();
    }
});

// Account information fields on the profile page start disabled; "Edit" unlocks them.
document.addEventListener('DOMContentLoaded', function () {
    var form = document.getElementById('profile-form');
    var editBtn = document.getElementById('profile-edit-btn');
    var saveBtn = document.getElementById('profile-save-btn');
    var cancelBtn = document.getElementById('profile-cancel-btn');

    if (!form || !editBtn || !saveBtn || !cancelBtn) {
        return;
    }

    var editableFields = [
        document.getElementById('p-first-name'),
        document.getElementById('p-last-name'),
        document.getElementById('p-email'),
        document.getElementById('p-phone')
    ];

    editBtn.addEventListener('click', function () {
        editableFields.forEach(function (field) {
            if (field) {
                field.disabled = false;
            }
        });
        editBtn.hidden = true;
        saveBtn.hidden = false;
        cancelBtn.hidden = false;
        if (editableFields[0]) {
            editableFields[0].focus();
        }
    });

    cancelBtn.addEventListener('click', function () {
        window.location.reload();
    });
});
