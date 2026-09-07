// Incidents list: clicking a row navigates to that incident's own page (no modal,
// unlike tickets). Incident detail: clicking a related-ticket row is handled by
// tickets.js itself (loaded on this page too, before incidents.js) — its own
// '.ticket-row' click wiring opens the ticket modal in place, so it never
// navigates away from the incident.
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.ticket-row[data-incident-id]').forEach(function (row) {
        row.addEventListener('click', function () {
            window.location.href = '/incidents/' + row.getAttribute('data-incident-id');
        });
    });

    // tickets.js defines this globally so the "press N" shortcut (wired in
    // common.js) can open its create-ticket modal from any page — but this page
    // only carries a CSRF-token stand-in under that id, not a real create form,
    // so undo the override and let the shortcut fall back to its normal
    // cross-page navigation instead of popping an empty dialog.
    window.openCreateTicketModal = null;

    // Same searchable multi-select chips behavior as the ticket create form's
    // candidates field — wireMultiCombobox is defined in tickets.js, which is
    // loaded on this page too (for the embedded ticket modal). An incident-derived
    // ticket is offered to candidates just like a regular one, not directly assigned.
    if (window.wireMultiCombobox) {
        window.wireMultiCombobox('it-candidates-input', 'it-candidates', 'it-candidates-options', 'it-candidates-chips', 'it-candidates-broadcast', 'it-candidates-clear');
    }

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

    // Report (comment) edit toggle — same disabled-until-edit pattern as the
    // incident info card, just per-row instead of a single form.
    document.querySelectorAll('.comment-edit-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var id = btn.getAttribute('data-comment-id');
            var view = document.getElementById('comment-view-' + id);
            var form = document.getElementById('comment-edit-form-' + id);
            if (view && form) {
                view.hidden = true;
                form.hidden = false;
                var textarea = form.querySelector('textarea');
                if (textarea) {
                    textarea.focus();
                }
            }
        });
    });

    document.querySelectorAll('.comment-edit-cancel').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var id = btn.getAttribute('data-comment-id');
            var view = document.getElementById('comment-view-' + id);
            var form = document.getElementById('comment-edit-form-' + id);
            if (view && form) {
                form.hidden = true;
                view.hidden = false;
            }
        });
    });

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
