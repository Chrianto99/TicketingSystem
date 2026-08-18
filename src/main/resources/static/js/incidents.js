// Incidents list: clicking a row navigates to that incident's own page (no modal,
// unlike tickets). Incident detail: clicking a related-ticket row opens that
// ticket's existing modal on the Tickets page via the same ?openTicket= convention
// tickets.js already reads on load.
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.ticket-row[data-incident-id]').forEach(function (row) {
        row.addEventListener('click', function () {
            window.location.href = '/incidents/' + row.getAttribute('data-incident-id');
        });
    });

    document.querySelectorAll('.ticket-row[data-ticket-id]').forEach(function (row) {
        row.addEventListener('click', function () {
            window.location.href = '/tickets?openTicket=' + row.getAttribute('data-ticket-id');
        });
    });

    // New-incident form starts collapsed behind a button; clicking it swaps the
    // button out for the form, and the form's own "Ακύρωση" button swaps back.
    var createToggle = document.getElementById('incident-create-toggle');
    var createCard = document.getElementById('incident-create-card');
    var createCancel = document.getElementById('incident-create-cancel');
    if (createToggle && createCard) {
        createToggle.addEventListener('click', function () {
            createToggle.hidden = true;
            createCard.hidden = false;
            document.getElementById('ic-subject').focus();
        });
    }
    if (createCancel && createCard && createToggle) {
        createCancel.addEventListener('click', function () {
            createCard.hidden = true;
            createToggle.hidden = false;
        });
    }

    // Incident info fields start disabled; "Επεξεργασία" unlocks them — same
    // pattern as the profile page's account-info edit toggle.
    var infoEditBtn = document.getElementById('ir-edit-btn');
    var infoSaveBtn = document.getElementById('ir-save-btn');
    var infoCancelBtn = document.getElementById('ir-cancel-btn');
    if (infoEditBtn && infoSaveBtn && infoCancelBtn) {
        var infoFields = [
            document.getElementById('ir-subject'),
            document.getElementById('ir-description'),
            document.getElementById('ir-priority')
        ];

        infoEditBtn.addEventListener('click', function () {
            infoFields.forEach(function (field) {
                if (field) {
                    field.disabled = false;
                }
            });
            infoEditBtn.hidden = true;
            infoSaveBtn.hidden = false;
            infoCancelBtn.hidden = false;
            if (infoFields[0]) {
                infoFields[0].focus();
            }
        });

        infoCancelBtn.addEventListener('click', function () {
            window.location.reload();
        });
    }

    // Comments / Tickets tabs below the info card — same tab pattern as the
    // statistics page and the ticket modal.
    document.querySelectorAll('.modal-tab').forEach(function (tab) {
        tab.addEventListener('click', function () {
            var target = tab.getAttribute('data-tab-target');
            document.querySelectorAll('.modal-tab').forEach(function (t) {
                var isTarget = t === tab;
                t.classList.toggle('active', isTarget);
                t.setAttribute('aria-selected', isTarget ? 'true' : 'false');
            });
            document.querySelectorAll('.modal-tab-panel').forEach(function (panel) {
                panel.style.display = panel.id === target ? '' : 'none';
            });
        });
    });

    // Incident attachments — same click-to-upload/delete flow as the ticket
    // modal (tickets.js wireAttachments), but the page is server-rendered
    // rather than a modal, so a successful mutation just reloads it.
    wireIncidentAttachments();

    function wireIncidentAttachments() {
        var toggleBtn = document.getElementById('ir-attachment-toggle');
        var input = document.getElementById('ir-attachment-input');
        var incidentId = document.body.getAttribute('data-incident-id');
        if (!toggleBtn || !input || !incidentId) {
            return;
        }

        function csrfHeaders() {
            var csrfInput = document.querySelector('#ir-edit-form input[name="_csrf"]');
            var headers = {};
            if (csrfInput) {
                headers['X-CSRF-TOKEN'] = csrfInput.value;
            }
            return headers;
        }

        toggleBtn.addEventListener('click', function () {
            input.click();
        });

        input.addEventListener('change', function () {
            if (!input.files || !input.files.length) {
                return;
            }
            var formData = new FormData();
            formData.append('file', input.files[0]);

            fetch('/api/incident-reports/' + incidentId + '/attachments', {
                method: 'POST',
                headers: csrfHeaders(),
                body: formData
            }).then(function (response) {
                if (!response.ok) {
                    return response.json().catch(function () {
                        return {};
                    }).then(function (body) {
                        throw new Error(body.error || Object.values(body)[0] || ('HTTP ' + response.status));
                    });
                }
                window.location.reload();
            }).catch(function (err) {
                window.showError('Αποτυχία επισύναψης αρχείου: ' + err.message);
            });
        });

        document.querySelectorAll('.attachment-delete').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var attachmentId = btn.getAttribute('data-attachment-id');
                window.showConfirm('Αφαίρεση αυτού του συνημμένου;').then(function (ok) {
                    if (!ok) {
                        return;
                    }
                    fetch('/api/attachments/' + attachmentId, {
                        method: 'DELETE',
                        headers: csrfHeaders()
                    }).then(function (response) {
                        if (!response.ok) {
                            return response.json().catch(function () {
                                return {};
                            }).then(function (body) {
                                throw new Error(body.error || Object.values(body)[0] || ('HTTP ' + response.status));
                            });
                        }
                        window.location.reload();
                    }).catch(function (err) {
                        window.showError('Αποτυχία αφαίρεσης συνημμένου: ' + err.message);
                    });
                });
            });
        });
    }
});
