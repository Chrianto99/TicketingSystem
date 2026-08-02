document.addEventListener('DOMContentLoaded', function () {
    var STATUS_LABELS = { OPEN: 'Ανοιχτό', RESOLVED: 'Επιλύθηκε', CANCELLED: 'Ακυρώθηκε' };
    var PRIORITY_LABELS = { LOW: 'Χαμηλή', MEDIUM: 'Μεσαία', HIGH: 'Υψηλή', CRITICAL: 'Κρίσιμη' };

    var modal = document.getElementById('ticket-modal');
    var modalTitle = document.getElementById('modal-title');
    var modalStatusBadge = document.getElementById('modal-status-badge');
    var modalPriorityBadge = document.getElementById('modal-priority-badge');
    var modalMeta = document.getElementById('modal-meta');
    var modalBody = document.getElementById('modal-body');
    var modalFooter = document.getElementById('modal-footer');
    var currentUserId = document.body.getAttribute('data-current-user-id');
    var actionModal = document.getElementById('action-modal');
    var actionModalForm = document.getElementById('action-modal-form');

    if (!modal) {
        return;
    }

    if (actionModal) {
        document.getElementById('action-modal-close').addEventListener('click', function () {
            actionModal.close();
        });
        document.getElementById('action-modal-cancel').addEventListener('click', function () {
            actionModal.close();
        });
        actionModalForm.addEventListener('submit', function () {
            document.getElementById('action-modal-submit').disabled = true;
        });
    }

    document.querySelectorAll('.ticket-row').forEach(function (row) {
        row.addEventListener('click', function () {
            openTicketModal(row.getAttribute('data-ticket-id'));
        });
    });

    var openTicketParam = new URLSearchParams(window.location.search).get('openTicket');
    if (openTicketParam) {
        openTicketModal(openTicketParam);
        var cleanUrl = window.location.pathname + window.location.hash;
        window.history.replaceState(null, '', cleanUrl);
    }

    function openTicketModal(ticketId) {
        modalTitle.textContent = 'Ticket #' + ticketId;
        modalStatusBadge.style.display = 'none';
        modalPriorityBadge.style.display = 'none';
        modalMeta.textContent = '';
        modalBody.innerHTML = '<p>Φόρτωση…</p>';
        modalFooter.innerHTML = '';
        modalFooter.style.display = 'none';
        modal.showModal();

        Promise.all([
            fetch('/api/tickets/' + ticketId).then(handleResponse),
            fetch('/api/tickets/' + ticketId + '/history').then(handleResponse)
        ]).then(function (results) {
            renderTicket(results[0], results[1]);
        }).catch(function (err) {
            modal.close();
            window.showError('Αποτυχία φόρτωσης ticket: ' + err.message);
        });
    }

    function handleResponse(response) {
        if (!response.ok) {
            throw new Error('HTTP ' + response.status);
        }
        return response.json();
    }

    function renderTicket(ticket, history) {
        modalTitle.textContent = 'Ticket #' + ticket.id;
        modalStatusBadge.textContent = STATUS_LABELS[ticket.status] || ticket.status;
        modalStatusBadge.className = 'badge status-' + ticket.status;
        modalStatusBadge.style.display = '';
        modalPriorityBadge.textContent = PRIORITY_LABELS[ticket.priority] || ticket.priority;
        modalPriorityBadge.className = 'badge priority-' + ticket.priority;
        modalPriorityBadge.style.display = '';
        modalMeta.textContent = 'Δημιουργήθηκε από ' + (ticket.creatorUsername || 'διαγραμμένο χρήστη') + ' ' + formatRelativeTime(ticket.createdAt);

        var canEdit = currentUserId && String(ticket.creatorId) === currentUserId;

        var html = '';

        html += '<div class="section-heading-row"><h3>ΠΛΗΡΟΦΟΡΙΕΣ</h3>';
        html += '<button type="button" class="icon-btn" id="td-info-toggle" title="Απόκρυψη" aria-expanded="true">▾</button>';
        if (canEdit) {
            html += '<button type="button" class="icon-btn" id="td-edit-toggle" title="Επεξεργασία">✎</button>';
        }
        html += '</div>';
        html += '<div id="info-fields">' + infoFieldsHtml(ticket) + '</div>';

        html += '<h3>Ιστορικό</h3>';
        if (!history.length) {
            html += '<p class="field-label" style="text-align:center;">Δεν υπάρχει ιστορικό ακόμα.</p>';
        } else {
            html += '<div class="timeline">';
            history.forEach(function (h) {
                html += historyItemHtml(h, ticket.id);
            });
            html += '</div>';
        }

        modalBody.innerHTML = html;
        wireReassign();
        wireCommentEdits();
        wireInfoCollapse();
        wireAttachments(ticket.id);

        if (canEdit) {
            document.getElementById('td-edit-toggle').addEventListener('click', function () {
                var fields = document.getElementById('info-fields');
                var infoToggle = document.getElementById('td-info-toggle');
                fields.style.display = '';
                infoToggle.textContent = '▾';
                infoToggle.title = 'Απόκρυψη';
                infoToggle.setAttribute('aria-expanded', 'true');
                fields.innerHTML = infoEditFormHtml(ticket);
                wireInfoEdit(ticket);
            });
        }

        modalFooter.innerHTML = actionAreaHtml(ticket);
        modalFooter.style.display = '';
        wireActionButtons(ticket);
    }

    function infoFieldsHtml(ticket) {
        var html = '';

        html += textField('Περίληψη', 'td-description', ticket.description);
        html += '<div class="form-row">' +
            textField('Όνομα καλούντος', 'td-caller-name', ticket.callerName) +
            textField('Αριθμός τηλεφώνου', 'td-phone', ticket.phoneNumber) +
            '</div>';

        html += '<div class="form-row">';
        html += textField('Τμήμα', 'td-department', ticket.departmentName);
        if (ticket.ipAddress) {
            html += textField('Διεύθυνση IP', 'td-ip', ticket.ipAddress);
        }
        html += '</div>';

        var categoryValue = ticket.category + (ticket.subcategory ? ' → ' + ticket.subcategory : '');
        html += textField('Κατηγορία Βλάβης', 'td-category', categoryValue);

        html += '<div class="form-group">';
        html += '<label for="td-assignee">Ανατέθηκε σε</label>';
        html += '<div class="assignee-view" id="assignee-view">';
        html += '<select id="td-assignee" disabled="disabled"><option selected="selected">' + escapeHtml(ticket.assignedUsername || 'Χωρίς ανάθεση') + '</option></select>';
        if (ticket.status === 'OPEN') {
            html += '<button type="button" class="icon-btn" id="td-reassign-toggle" title="Επανανάθεση">✎</button>';
        }
        html += '</div>';
        if (ticket.status === 'OPEN') {
            var csrfInputForReassign = document.querySelector('#create-ticket-modal input[name="_csrf"]');
            var reassignCsrfToken = csrfInputForReassign ? csrfInputForReassign.value : '';
            html += '<form id="td-reassign-form" class="assignee-edit" method="post" action="/tickets/' + ticket.id + '/reassign" style="display:none;">';
            html += '<input type="hidden" name="_csrf" value="' + escapeHtml(reassignCsrfToken) + '">';
            html += '<select name="assignedTo" id="td-reassign-select"></select>';
            html += '<input type="text" name="commentText" placeholder="Λόγος (υποχρεωτικό)" required="required">';
            html += '<button type="submit" class="btn btn-primary btn-sm">Αποθήκευση</button>';
            html += '<button type="button" class="btn btn-sm" id="td-reassign-cancel">Άκυρο</button>';
            html += '</form>';
        }
        html += '</div>';

        return html;
    }

    function infoEditFormHtml(ticket) {
        var csrfInput = document.querySelector('#create-ticket-modal input[name="_csrf"]');
        var csrfToken = csrfInput ? csrfInput.value : '';

        var html = '<form id="td-edit-form" class="ticket-form" method="post" action="/tickets/' + ticket.id + '/edit">';
        html += '<input type="hidden" name="_csrf" value="' + escapeHtml(csrfToken) + '">';

        html += '<div class="form-group">';
        html += '<label for="td-edit-description">Περίληψη</label>';
        html += '<textarea id="td-edit-description" name="description" required="required">' + escapeHtml(ticket.description) + '</textarea>';
        html += '</div>';

        html += '<div class="form-row">';
        html += '<div class="form-group"><label for="td-edit-caller-name">Όνομα καλούντος</label>' +
            '<input type="text" id="td-edit-caller-name" name="callerName" value="' + escapeHtml(ticket.callerName) + '" required="required"></div>';
        html += '<div class="form-group"><label for="td-edit-phone">Αριθμός τηλεφώνου</label>' +
            '<input type="text" id="td-edit-phone" name="phoneNumber" value="' + escapeHtml(ticket.phoneNumber) + '" required="required"></div>';
        html += '</div>';

        html += '<div class="form-row">';
        html += '<div class="form-group"><label for="td-edit-department">Τμήμα</label>' +
            '<select id="td-edit-department" name="departmentId" required="required"></select></div>';
        html += '<div class="form-group"><label for="td-edit-ip">Διεύθυνση IP</label>' +
            '<input type="text" id="td-edit-ip" name="ipAddress" value="' + escapeHtml(ticket.ipAddress || '') + '" placeholder="Προαιρετικό" ' +
            'pattern="^((25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)$" title="Πρέπει να είναι έγκυρη διεύθυνση IPv4"></div>';
        html += '</div>';

        html += '<div class="form-row">';
        html += '<div class="form-group"><label for="td-edit-category">Κατηγορία Βλάβης</label>' +
            '<select id="td-edit-category" name="categoryId" required="required"></select></div>';
        html += '<div class="form-group"><label for="td-edit-subcategory">Υποκατηγορία</label>' +
            '<select id="td-edit-subcategory" name="subcategoryId"><option value="">Επιλέξτε πρώτα κατηγορία βλάβης</option></select></div>';
        html += '</div>';

        html += '<div class="form-group"><label for="td-edit-priority">Προτεραιότητα</label>' +
            '<select id="td-edit-priority" name="priority" required="required"></select></div>';

        html += '<div class="ticket-action-buttons">';
        html += '<button type="button" class="btn" id="td-edit-cancel-btn">Άκυρο</button>';
        html += '<button type="submit" class="btn btn-primary">Αποθήκευση</button>';
        html += '</div>';
        html += '</form>';
        return html;
    }

    function wireInfoEdit(ticket) {
        var deptSelect = document.getElementById('td-edit-department');
        var catSelect = document.getElementById('td-edit-category');
        var subcatSelect = document.getElementById('td-edit-subcategory');
        var prioritySelect = document.getElementById('td-edit-priority');
        var cancelBtn = document.getElementById('td-edit-cancel-btn');

        cloneOptionsExcludingBlank('#ct-department', deptSelect);
        ensureCurrentOptionSelected(deptSelect, ticket.departmentId, ticket.departmentName);

        cloneOptionsExcludingBlank('#ct-category', catSelect);
        ensureCurrentOptionSelected(catSelect, ticket.categoryId, ticket.category);

        cloneOptionsExcludingBlank('#ct-priority', prioritySelect);
        prioritySelect.value = ticket.priority;

        loadEditSubcategories(ticket.categoryId, ticket.subcategoryId);

        catSelect.addEventListener('change', function () {
            loadEditSubcategories(catSelect.value, null);
        });

        cancelBtn.addEventListener('click', function () {
            document.getElementById('info-fields').innerHTML = infoFieldsHtml(ticket);
            wireReassign();
        });
    }

    function cloneOptionsExcludingBlank(sourceSelector, targetSelect) {
        targetSelect.innerHTML = '';
        document.querySelectorAll(sourceSelector + ' option').forEach(function (opt) {
            if (!opt.value) {
                return;
            }
            var clone = opt.cloneNode(true);
            clone.selected = false;
            targetSelect.appendChild(clone);
        });
    }

    function ensureCurrentOptionSelected(selectEl, id, label) {
        if (!id) {
            return;
        }
        selectEl.value = id;
        if (String(selectEl.value) !== String(id)) {
            var opt = document.createElement('option');
            opt.value = id;
            opt.textContent = (label || 'Άγνωστο') + ' (Ανενεργό)';
            selectEl.appendChild(opt);
            selectEl.value = id;
        }
    }

    function loadEditSubcategories(categoryId, selectedId) {
        var subcatSelect = document.getElementById('td-edit-subcategory');
        if (!categoryId) {
            subcatSelect.innerHTML = '<option value="">Επιλέξτε πρώτα κατηγορία βλάβης</option>';
            subcatSelect.disabled = true;
            return;
        }
        subcatSelect.disabled = true;
        subcatSelect.innerHTML = '<option value="">Φόρτωση…</option>';

        fetch('/api/subcategories?categoryId=' + encodeURIComponent(categoryId))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(function (subcategories) {
                var html = '<option value="">Προαιρετικό</option>';
                subcategories.forEach(function (sc) {
                    if (!sc.active && String(sc.id) !== String(selectedId)) {
                        return;
                    }
                    html += '<option value="' + sc.id + '">' + escapeHtml(sc.name) + (sc.active ? '' : ' (Ανενεργή)') + '</option>';
                });
                subcatSelect.innerHTML = html;
                if (selectedId) {
                    subcatSelect.value = selectedId;
                }
                subcatSelect.disabled = false;
            })
            .catch(function () {
                subcatSelect.innerHTML = '<option value="">Αποτυχία φόρτωσης υποκατηγοριών</option>';
            });
    }

    function wireReassign() {
        var toggle = document.getElementById('td-reassign-toggle');
        if (!toggle) {
            return;
        }
        var view = document.getElementById('assignee-view');
        var form = document.getElementById('td-reassign-form');
        var select = document.getElementById('td-reassign-select');
        var cancelBtn = document.getElementById('td-reassign-cancel');

        var sourceOptions = document.querySelectorAll('#ct-assignee option');
        select.innerHTML = '';
        sourceOptions.forEach(function (opt) {
            var clone = opt.cloneNode(true);
            clone.selected = false;
            select.appendChild(clone);
        });

        toggle.addEventListener('click', function () {
            view.style.display = 'none';
            form.style.display = 'flex';
        });

        cancelBtn.addEventListener('click', function () {
            form.style.display = 'none';
            view.style.display = '';
        });
    }

    function wireInfoCollapse() {
        var toggle = document.getElementById('td-info-toggle');
        var fields = document.getElementById('info-fields');

        toggle.addEventListener('click', function () {
            var collapsed = fields.style.display === 'none';
            fields.style.display = collapsed ? '' : 'none';
            toggle.textContent = collapsed ? '▾' : '▸';
            toggle.title = collapsed ? 'Απόκρυψη' : 'Εμφάνιση';
            toggle.setAttribute('aria-expanded', collapsed ? 'true' : 'false');
        });
    }

    function actionAreaHtml(ticket) {
        var isOpen = ticket.status === 'OPEN';
        var html = '<div class="ticket-action-area">';

        if (isOpen) {
            html += '<div class="ticket-action-buttons ticket-action-buttons-spread">';
            html += '<button type="button" class="btn btn-danger" id="ticket-cancel-btn">✗ Ακύρωση</button>';
            html += '<button type="button" class="btn btn-success" id="ticket-resolve-btn">✓ Επίλυση</button>';
            html += '<button type="button" class="btn" id="ticket-comment-btn">💬 Σχόλιο</button>';
            html += '</div>';
        } else {
            var isCancelled = ticket.status === 'CANCELLED';
            var isAdmin = document.body.getAttribute('data-is-admin') === 'true';

            html += '<div class="ticket-action-buttons ticket-action-buttons-center">';
            html += '<button type="button" class="btn btn-primary" id="ticket-reopen-btn">↺ Επανάνοιγμα</button>';
            if (isCancelled && isAdmin) {
                html += '<button type="button" class="btn btn-danger" id="ticket-delete-btn">🗑 Διαγραφή</button>';
            }
            html += '</div>';
        }

        html += '</div>';
        return html;
    }

    function wireActionButtons(ticket) {
        var isOpen = ticket.status === 'OPEN';

        if (isOpen) {
            document.getElementById('ticket-comment-btn').addEventListener('click', function () {
                openActionModal({
                    action: '/tickets/' + ticket.id + '/comments',
                    title: 'Προσθήκη Σχολίου',
                    label: 'Σχόλιο',
                    placeholder: 'Προσθέστε ένα σχόλιο…',
                    submitLabel: 'Σχόλιο',
                    needsSubcategory: false
                });
            });

            document.getElementById('ticket-resolve-btn').addEventListener('click', function () {
                openActionModal({
                    action: '/tickets/' + ticket.id + '/resolve',
                    title: 'Επίλυση Ticket',
                    label: 'Λύση',
                    placeholder: 'Περιγράψτε τη λύση…',
                    submitLabel: 'Επίλυση',
                    needsSubcategory: !ticket.subcategoryId,
                    categoryId: ticket.categoryId
                });
            });

            document.getElementById('ticket-cancel-btn').addEventListener('click', function () {
                openActionModal({
                    action: '/tickets/' + ticket.id + '/cancel',
                    title: 'Ακύρωση Ticket',
                    label: 'Λόγος ακύρωσης',
                    placeholder: 'Περιγράψτε τον λόγο ακύρωσης…',
                    submitLabel: 'Ακύρωση Ticket',
                    needsSubcategory: false
                });
            });
        } else {
            document.getElementById('ticket-reopen-btn').addEventListener('click', function () {
                openActionModal({
                    action: '/tickets/' + ticket.id + '/reopen',
                    title: 'Επανάνοιγμα Ticket',
                    label: 'Λόγος επανανοίγματος',
                    placeholder: 'Περιγράψτε γιατί το επανανοίγετε…',
                    submitLabel: 'Επανάνοιγμα',
                    needsSubcategory: false
                });
            });

            var deleteBtn = document.getElementById('ticket-delete-btn');
            if (deleteBtn) {
                deleteBtn.addEventListener('click', function () {
                    window.showConfirm('Διαγραφή αυτού του ακυρωμένου ticket; Αυτή η ενέργεια δεν μπορεί να αναιρεθεί.').then(function (ok) {
                        if (!ok) {
                            return;
                        }
                        var csrfInput = document.querySelector('#create-ticket-modal input[name="_csrf"]');
                        var headers = {};
                        if (csrfInput) {
                            headers['X-CSRF-TOKEN'] = csrfInput.value;
                        }
                        fetch('/api/tickets/' + ticket.id, { method: 'DELETE', headers: headers })
                            .then(function (response) {
                                if (!response.ok) {
                                    return response.json().catch(function () {
                                        return {};
                                    }).then(function (body) {
                                        throw new Error(body.error || Object.values(body)[0] || ('HTTP ' + response.status));
                                    });
                                }
                                modal.close();
                                window.location.reload();
                            })
                            .catch(function (err) {
                                window.showError('Αποτυχία διαγραφής ticket: ' + err.message);
                            });
                    });
                });
            }
        }
    }

    function openActionModal(config) {
        var form = document.getElementById('action-modal-form');
        var titleEl = document.getElementById('action-modal-title');
        var labelEl = document.getElementById('action-modal-comment-label');
        var textarea = document.getElementById('action-modal-comment');
        var submitBtn = document.getElementById('action-modal-submit');
        var subcatGroup = document.getElementById('action-modal-subcategory-group');
        var subcatSelect = document.getElementById('action-modal-subcategory');
        var csrfInput = document.getElementById('action-modal-csrf');
        var sourceCsrf = document.querySelector('#create-ticket-modal input[name="_csrf"]');

        form.setAttribute('action', config.action);
        titleEl.textContent = config.title;
        labelEl.textContent = config.label;
        textarea.value = '';
        textarea.placeholder = config.placeholder || '';
        submitBtn.textContent = config.submitLabel || 'Επιβεβαίωση';
        submitBtn.disabled = false;
        csrfInput.value = sourceCsrf ? sourceCsrf.value : '';

        if (config.needsSubcategory) {
            subcatGroup.style.display = '';
            subcatSelect.required = true;
            loadActionModalSubcategories(config.categoryId);
        } else {
            subcatGroup.style.display = 'none';
            subcatSelect.required = false;
            subcatSelect.innerHTML = '';
        }

        actionModal.showModal();
        textarea.focus();
    }

    function loadActionModalSubcategories(categoryId) {
        var subcatSelect = document.getElementById('action-modal-subcategory');
        subcatSelect.disabled = true;
        subcatSelect.innerHTML = '<option value="">Φόρτωση…</option>';

        fetch('/api/subcategories?categoryId=' + encodeURIComponent(categoryId))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(function (subcategories) {
                var active = subcategories.filter(function (sc) { return sc.active; });
                var html;
                if (active.length) {
                    html = '<option value="">Επιλέξτε υποκατηγορία…</option>';
                    active.forEach(function (sc) {
                        html += '<option value="' + sc.id + '">' + escapeHtml(sc.name) + '</option>';
                    });
                } else {
                    html = '<option value="">Δεν υπάρχουν διαθέσιμες υποκατηγορίες</option>';
                }
                subcatSelect.innerHTML = html;
                subcatSelect.disabled = false;
            })
            .catch(function () {
                subcatSelect.innerHTML = '<option value="">Αποτυχία φόρτωσης υποκατηγοριών</option>';
            });
    }

    function historyItemHtml(h, ticketId) {
        var performer = escapeHtml(h.performedByUsername || 'Διαγραμμένος χρήστης');
        var assignee = escapeHtml(h.assignedToUsername || 'διαγραμμένο χρήστη');
        var dotClass = '';
        var text;

        switch (h.action) {
            case 'CREATED':
                text = 'Δημιουργήθηκε από ' + performer;
                break;
            case 'ASSIGNED':
                text = performer + ' ανέθεσε το ticket σε ' + assignee + (h.commentText ? ' και σχολίασε:' : '.');
                break;
            case 'REASSIGNED':
                text = performer + ' επανέθεσε το ticket σε ' + assignee + (h.commentText ? ' και σχολίασε:' : '.');
                break;
            case 'COMMENT_ADDED':
                text = performer + ' σχολίασε:';
                dotClass = 'dot-comment';
                break;
            case 'RESOLVED':
                text = performer + ' επέλυσε το ticket' + (h.commentText ? ' με λύση:' : '.');
                dotClass = 'dot-resolved';
                break;
            case 'CANCELLED':
                text = performer + ' ακύρωσε το ticket' + (h.commentText ? ' με λόγο:' : '.');
                dotClass = 'dot-cancelled';
                break;
            case 'REOPENED':
                text = performer + ' επανάνοιξε το ticket' + (h.commentText ? ' με λόγο:' : '.');
                break;
            default:
                text = performer + ' ενημέρωσε το ticket.';
        }

        var itemHtml = '<div class="timeline-item">';
        itemHtml += '<div class="timeline-dot ' + dotClass + '"></div>';
        itemHtml += '<p class="timeline-text">' + text + '</p>';
        if (h.commentText) {
            var canEditComment = h.commentId && currentUserId && String(h.performedById) === currentUserId;
            itemHtml += '<div class="timeline-quote-wrap" id="quote-view-' + h.commentId + '">';
            itemHtml += '<p class="timeline-quote">“' + escapeHtml(h.commentText) + '”</p>';
            itemHtml += '<button type="button" class="icon-btn attachment-toggle" data-comment-id="' + h.commentId + '" title="Επισύναψη αρχείου">📎</button>';
            if (canEditComment) {
                itemHtml += '<button type="button" class="icon-btn comment-edit-toggle" data-comment-id="' + h.commentId + '" title="Επεξεργασία σχολίου">✎</button>';
            }
            itemHtml += '</div>';
            itemHtml += '<input type="file" class="attachment-input" data-comment-id="' + h.commentId + '" hidden>';
            itemHtml += attachmentsHtml(h.attachments);
            if (canEditComment) {
                var csrfInputForComment = document.querySelector('#create-ticket-modal input[name="_csrf"]');
                var commentCsrfToken = csrfInputForComment ? csrfInputForComment.value : '';
                itemHtml += '<form id="comment-edit-' + h.commentId + '" class="comment-edit-form" method="post" ' +
                    'action="/tickets/' + ticketId + '/comments/' + h.commentId + '/edit" style="display:none;">';
                itemHtml += '<input type="hidden" name="_csrf" value="' + escapeHtml(commentCsrfToken) + '">';
                itemHtml += '<textarea name="text" required="required">' + escapeHtml(h.commentText) + '</textarea>';
                itemHtml += '<div class="ticket-action-buttons">';
                itemHtml += '<button type="button" class="btn btn-sm comment-edit-cancel" data-comment-id="' + h.commentId + '">Άκυρο</button>';
                itemHtml += '<button type="submit" class="btn btn-primary btn-sm">Αποθήκευση</button>';
                itemHtml += '</div>';
                itemHtml += '</form>';
            }
        }
        itemHtml += '<p class="timeline-time">' + formatDate(h.timestamp) + '</p>';
        itemHtml += '</div>';
        return itemHtml;
    }

    function attachmentsHtml(attachments) {
        if (!attachments || !attachments.length) {
            return '';
        }
        var html = '<div class="attachment-list">';
        attachments.forEach(function (a) {
            var canDelete = currentUserId && a.uploadedById && String(a.uploadedById) === currentUserId;
            html += '<span class="attachment-chip">';
            html += '<a href="/api/attachments/' + a.id + '">📎 ' + escapeHtml(a.fileName) + '</a>';
            if (canDelete) {
                html += '<button type="button" class="attachment-delete" data-attachment-id="' + a.id + '" title="Αφαίρεση συνημμένου">✕</button>';
            }
            html += '</span>';
        });
        html += '</div>';
        return html;
    }

    function wireAttachments(ticketId) {
        document.querySelectorAll('.attachment-toggle').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var commentId = btn.getAttribute('data-comment-id');
                document.querySelector('.attachment-input[data-comment-id="' + commentId + '"]').click();
            });
        });

        document.querySelectorAll('.attachment-input').forEach(function (input) {
            input.addEventListener('change', function () {
                if (!input.files || !input.files.length) {
                    return;
                }
                var commentId = input.getAttribute('data-comment-id');
                var formData = new FormData();
                formData.append('file', input.files[0]);

                var csrfInput = document.querySelector('#create-ticket-modal input[name="_csrf"]');
                var headers = {};
                if (csrfInput) {
                    formData.append('_csrf', csrfInput.value);
                    headers['X-CSRF-TOKEN'] = csrfInput.value;
                }

                fetch('/api/comments/' + commentId + '/attachments', {
                    method: 'POST',
                    headers: headers,
                    body: formData
                }).then(function (response) {
                    if (!response.ok) {
                        return response.json().catch(function () {
                            return {};
                        }).then(function (body) {
                            throw new Error(body.error || Object.values(body)[0] || ('HTTP ' + response.status));
                        });
                    }
                    return response.json();
                }).then(function () {
                    openTicketModal(ticketId);
                }).catch(function (err) {
                    window.showError('Αποτυχία επισύναψης αρχείου: ' + err.message);
                });
            });
        });

        document.querySelectorAll('.attachment-delete').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var attachmentId = btn.getAttribute('data-attachment-id');
                window.showConfirm('Αφαίρεση αυτού του συνημμένου;').then(function (ok) {
                    if (!ok) {
                        return;
                    }
                    var csrfInput = document.querySelector('#create-ticket-modal input[name="_csrf"]');
                    var headers = {};
                    if (csrfInput) {
                        headers['X-CSRF-TOKEN'] = csrfInput.value;
                    }
                    fetch('/api/attachments/' + attachmentId, {
                        method: 'DELETE',
                        headers: headers
                    }).then(function (response) {
                        if (!response.ok) {
                            return response.json().catch(function () {
                                return {};
                            }).then(function (body) {
                                throw new Error(body.error || Object.values(body)[0] || ('HTTP ' + response.status));
                            });
                        }
                        openTicketModal(ticketId);
                    }).catch(function (err) {
                        window.showError('Αποτυχία αφαίρεσης συνημμένου: ' + err.message);
                    });
                });
            });
        });
    }

    function wireCommentEdits() {
        document.querySelectorAll('.comment-edit-toggle').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var commentId = btn.getAttribute('data-comment-id');
                document.getElementById('quote-view-' + commentId).style.display = 'none';
                document.getElementById('comment-edit-' + commentId).style.display = 'flex';
            });
        });
        document.querySelectorAll('.comment-edit-cancel').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var commentId = btn.getAttribute('data-comment-id');
                document.getElementById('comment-edit-' + commentId).style.display = 'none';
                document.getElementById('quote-view-' + commentId).style.display = 'flex';
            });
        });
    }

    function textField(label, id, value) {
        return '<div class="form-group">' +
            '<label for="' + id + '">' + escapeHtml(label) + '</label>' +
            '<input type="text" id="' + id + '" value="' + escapeHtml(value || '') + '" disabled="disabled">' +
            '</div>';
    }

    function formatRelativeTime(isoString) {
        if (!isoString) {
            return 'πριν από άγνωστο χρονικό διάστημα';
        }
        var then = new Date(isoString).getTime();
        if (isNaN(then)) {
            return 'πριν από άγνωστο χρονικό διάστημα';
        }
        var seconds = Math.floor((Date.now() - then) / 1000);
        if (seconds < 60) {
            return 'μόλις τώρα';
        }
        var units = [
            ['έτος', 'χρόνια', 31536000],
            ['μήνα', 'μήνες', 2592000],
            ['εβδομάδα', 'εβδομάδες', 604800],
            ['μέρα', 'μέρες', 86400],
            ['ώρα', 'ώρες', 3600],
            ['λεπτό', 'λεπτά', 60]
        ];
        for (var i = 0; i < units.length; i++) {
            var value = Math.floor(seconds / units[i][2]);
            if (value >= 1) {
                return value + ' ' + (value === 1 ? units[i][0] : units[i][1]) + ' πριν';
            }
        }
        return 'μόλις τώρα';
    }

    function formatDate(isoString) {
        if (!isoString) {
            return '—';
        }
        var d = new Date(isoString);
        if (isNaN(d.getTime())) {
            return isoString;
        }
        return d.toLocaleString();
    }

    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
});

document.addEventListener('DOMContentLoaded', function () {
    var createModal = document.getElementById('create-ticket-modal');
    if (createModal && createModal.getAttribute('data-has-errors') === 'true') {
        createModal.showModal();
    }

    // Shared open trigger for the toolbar button, the global "N" shortcut (wired in
    // common.js so it works from any page), and the ?openCreate=true redirect used
    // when the shortcut is pressed somewhere other than /tickets.
    if (createModal) {
        window.openCreateTicketModal = function () {
            createModal.showModal();
            var phoneInput = document.getElementById('ct-phone');
            if (phoneInput) {
                phoneInput.focus();
            }
        };

        if (new URLSearchParams(window.location.search).get('openCreate')) {
            window.openCreateTicketModal();
            var cleanCreateUrl = window.location.pathname + window.location.hash;
            window.history.replaceState(null, '', cleanCreateUrl);
        }
    }

    var categorySelect = document.getElementById('ct-category');
    var subcategorySelect = document.getElementById('ct-subcategory');

    if (!categorySelect || !subcategorySelect) {
        return;
    }

    categorySelect.addEventListener('change', function () {
        loadSubcategories(categorySelect.value, null);
    });

    var initialSubcategoryId = subcategorySelect.getAttribute('data-initial-value');
    if (categorySelect.value) {
        loadSubcategories(categorySelect.value, initialSubcategoryId);
    }

    function loadSubcategories(categoryId, selectedValue) {
        if (!categoryId) {
            subcategorySelect.innerHTML = '<option value="">Επιλέξτε πρώτα κατηγορία βλάβης</option>';
            subcategorySelect.disabled = true;
            return;
        }

        subcategorySelect.disabled = true;
        subcategorySelect.innerHTML = '<option value="">Φόρτωση…</option>';

        fetch('/api/subcategories?categoryId=' + encodeURIComponent(categoryId))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(function (subcategories) {
                var html = '<option value="">Προαιρετικό</option>';
                subcategories.forEach(function (sc) {
                    if (!sc.active && String(sc.id) !== String(selectedValue)) {
                        return;
                    }
                    html += '<option value="' + sc.id + '">' + escapeSubcategoryHtml(sc.name) + (sc.active ? '' : ' (Ανενεργή)') + '</option>';
                });
                subcategorySelect.innerHTML = html;
                if (selectedValue) {
                    subcategorySelect.value = selectedValue;
                }
                subcategorySelect.disabled = false;
            })
            .catch(function () {
                subcategorySelect.innerHTML = '<option value="">Αποτυχία φόρτωσης υποκατηγοριών</option>';
            });
    }

    function escapeSubcategoryHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
});

// Department combobox: a text input backed by the (hidden) #ct-department <select>
// so the user can type to filter, but the submitted value must match an existing option.
document.addEventListener('DOMContentLoaded', function () {
    var input = document.getElementById('ct-department-input');
    var select = document.getElementById('ct-department');
    var optionsList = document.getElementById('ct-department-options');

    if (!input || !select || !optionsList) {
        return;
    }

    var options = Array.prototype.slice.call(select.options)
        .filter(function (opt) { return opt.value; })
        .map(function (opt) { return { value: opt.value, label: opt.text }; });

    var activeIndex = -1;

    // Pre-fill from a server-rendered selection (e.g. re-showing the form after a validation error).
    if (select.value) {
        var selected = options.filter(function (o) { return o.value === select.value; })[0];
        if (selected) {
            input.value = selected.label;
        }
    }

    input.addEventListener('input', function () {
        select.value = '';
        renderOptions(input.value.trim().toLowerCase());
    });

    input.addEventListener('focus', function () {
        renderOptions(input.value.trim().toLowerCase());
    });

    input.addEventListener('keydown', function (e) {
        var items = optionsList.querySelectorAll('li');
        if (!items.length || optionsList.hidden) {
            return;
        }
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            activeIndex = Math.min(activeIndex + 1, items.length - 1);
            highlight(items);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            activeIndex = Math.max(activeIndex - 1, 0);
            highlight(items);
        } else if (e.key === 'Enter') {
            if (activeIndex >= 0 && items[activeIndex]) {
                e.preventDefault();
                choose(items[activeIndex].getAttribute('data-value'), items[activeIndex].textContent);
            }
        } else if (e.key === 'Escape') {
            closeOptions();
        }
    });

    input.addEventListener('blur', function () {
        // Delay so a click on an option registers before the list closes.
        setTimeout(function () {
            closeOptions();
            var current = options.filter(function (o) { return o.value === select.value; })[0];
            input.value = current ? current.label : '';
        }, 150);
    });

    function renderOptions(query) {
        var matches = options.filter(function (o) { return o.label.toLowerCase().indexOf(query) !== -1; });
        activeIndex = -1;
        if (!matches.length) {
            closeOptions();
            return;
        }
        optionsList.innerHTML = matches.map(function (o) {
            return '<li data-value="' + o.value + '">' + escapeSubcategoryHtml(o.label) + '</li>';
        }).join('');
        optionsList.hidden = false;
    }

    function highlight(items) {
        items.forEach(function (item, i) {
            item.classList.toggle('active', i === activeIndex);
        });
        if (items[activeIndex]) {
            items[activeIndex].scrollIntoView({ block: 'nearest' });
        }
    }

    function closeOptions() {
        optionsList.hidden = true;
        optionsList.innerHTML = '';
        activeIndex = -1;
    }

    function choose(value, label) {
        select.value = value;
        input.value = label;
        closeOptions();
    }

    optionsList.addEventListener('mousedown', function (e) {
        var li = e.target.closest('li');
        if (!li) {
            return;
        }
        e.preventDefault();
        choose(li.getAttribute('data-value'), li.textContent);
    });

    function escapeSubcategoryHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
});
