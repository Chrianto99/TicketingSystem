document.addEventListener('DOMContentLoaded', function () {
    var modal = document.getElementById('ticket-modal');
    var modalTitle = document.getElementById('modal-title');
    var modalStatusBadge = document.getElementById('modal-status-badge');
    var modalPriorityBadge = document.getElementById('modal-priority-badge');
    var modalMeta = document.getElementById('modal-meta');
    var modalBody = document.getElementById('modal-body');
    var modalFooter = document.getElementById('modal-footer');
    var currentUserId = document.body.getAttribute('data-current-user-id');

    if (!modal) {
        return;
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
        modalBody.innerHTML = '<p>Loading…</p>';
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
            window.showError('Failed to load ticket: ' + err.message);
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
        modalStatusBadge.textContent = ticket.status;
        modalStatusBadge.className = 'badge status-' + ticket.status;
        modalStatusBadge.style.display = '';
        modalPriorityBadge.textContent = ticket.priority;
        modalPriorityBadge.className = 'badge priority-' + ticket.priority;
        modalPriorityBadge.style.display = '';
        modalMeta.textContent = 'Opened ' + formatRelativeTime(ticket.createdAt) + ' by ' + (ticket.creatorUsername || 'a deleted user');

        var canEdit = currentUserId && String(ticket.creatorId) === currentUserId;

        var html = '';

        html += '<div class="section-heading-row"><h3>INFO</h3>';
        html += '<button type="button" class="icon-btn" id="td-info-toggle" title="Hide" aria-expanded="true">▾</button>';
        if (canEdit) {
            html += '<button type="button" class="icon-btn" id="td-edit-toggle" title="Edit">✎</button>';
        }
        html += '</div>';
        html += '<div id="info-fields">' + infoFieldsHtml(ticket) + '</div>';

        html += '<h3>History</h3>';
        if (!history.length) {
            html += '<p class="field-label" style="text-align:center;">No history yet.</p>';
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

        if (canEdit) {
            document.getElementById('td-edit-toggle').addEventListener('click', function () {
                var fields = document.getElementById('info-fields');
                var infoToggle = document.getElementById('td-info-toggle');
                fields.style.display = '';
                infoToggle.textContent = '▾';
                infoToggle.title = 'Hide';
                infoToggle.setAttribute('aria-expanded', 'true');
                fields.innerHTML = infoEditFormHtml(ticket);
                wireInfoEdit(ticket);
            });
        }

        modalFooter.innerHTML = actionAreaHtml(ticket);
        modalFooter.style.display = '';
        wireActionConfirms(ticket);
    }

    function infoFieldsHtml(ticket) {
        var html = '';

        html += textField('Summary', 'td-description', ticket.description);
        html += '<div class="form-row">' +
            textField('Caller name', 'td-caller-name', ticket.callerName) +
            textField('Phone number', 'td-phone', ticket.phoneNumber) +
            '</div>';

        html += '<div class="form-row">';
        html += textField('Department', 'td-department', ticket.departmentName);
        if (ticket.ipAddress) {
            html += textField('IP address', 'td-ip', ticket.ipAddress);
        }
        html += '</div>';

        var categoryValue = ticket.category + (ticket.subcategory ? ' → ' + ticket.subcategory : '');
        html += textField('Category', 'td-category', categoryValue);

        html += '<div class="form-group">';
        html += '<label for="td-assignee">Assigned to</label>';
        html += '<div class="assignee-view" id="assignee-view">';
        html += '<select id="td-assignee" disabled="disabled"><option selected="selected">' + escapeHtml(ticket.assignedUsername || 'Unassigned') + '</option></select>';
        if (ticket.status === 'OPEN') {
            html += '<button type="button" class="icon-btn" id="td-reassign-toggle" title="Reassign">✎</button>';
        }
        html += '</div>';
        if (ticket.status === 'OPEN') {
            var csrfInputForReassign = document.querySelector('#create-ticket-modal input[name="_csrf"]');
            var reassignCsrfToken = csrfInputForReassign ? csrfInputForReassign.value : '';
            html += '<form id="td-reassign-form" class="assignee-edit" method="post" action="/tickets/' + ticket.id + '/reassign" style="display:none;">';
            html += '<input type="hidden" name="_csrf" value="' + escapeHtml(reassignCsrfToken) + '">';
            html += '<select name="assignedTo" id="td-reassign-select"></select>';
            html += '<input type="text" name="commentText" placeholder="Reason (required)" required="required">';
            html += '<button type="submit" class="btn btn-primary btn-sm">Save</button>';
            html += '<button type="button" class="btn btn-sm" id="td-reassign-cancel">Cancel</button>';
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
        html += '<label for="td-edit-description">Summary</label>';
        html += '<textarea id="td-edit-description" name="description" required="required">' + escapeHtml(ticket.description) + '</textarea>';
        html += '</div>';

        html += '<div class="form-row">';
        html += '<div class="form-group"><label for="td-edit-caller-name">Caller name</label>' +
            '<input type="text" id="td-edit-caller-name" name="callerName" value="' + escapeHtml(ticket.callerName) + '" required="required"></div>';
        html += '<div class="form-group"><label for="td-edit-phone">Phone number</label>' +
            '<input type="text" id="td-edit-phone" name="phoneNumber" value="' + escapeHtml(ticket.phoneNumber) + '" required="required"></div>';
        html += '</div>';

        html += '<div class="form-row">';
        html += '<div class="form-group"><label for="td-edit-department">Department</label>' +
            '<select id="td-edit-department" name="departmentId" required="required"></select></div>';
        html += '<div class="form-group"><label for="td-edit-ip">IP address</label>' +
            '<input type="text" id="td-edit-ip" name="ipAddress" value="' + escapeHtml(ticket.ipAddress || '') + '" placeholder="Optional" ' +
            'pattern="^((25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)$" title="Must be a valid IPv4 address"></div>';
        html += '</div>';

        html += '<div class="form-row">';
        html += '<div class="form-group"><label for="td-edit-category">Category</label>' +
            '<select id="td-edit-category" name="categoryId" required="required"></select></div>';
        html += '<div class="form-group"><label for="td-edit-subcategory">Subcategory</label>' +
            '<select id="td-edit-subcategory" name="subcategoryId"><option value="">Select a category first</option></select></div>';
        html += '</div>';

        html += '<div class="ticket-action-buttons">';
        html += '<button type="button" class="btn" id="td-edit-cancel-btn">Cancel</button>';
        html += '<button type="submit" class="btn btn-primary">Save</button>';
        html += '</div>';
        html += '</form>';
        return html;
    }

    function wireInfoEdit(ticket) {
        var deptSelect = document.getElementById('td-edit-department');
        var catSelect = document.getElementById('td-edit-category');
        var subcatSelect = document.getElementById('td-edit-subcategory');
        var cancelBtn = document.getElementById('td-edit-cancel-btn');

        cloneOptionsExcludingBlank('#ct-department', deptSelect);
        ensureCurrentOptionSelected(deptSelect, ticket.departmentId, ticket.departmentName);

        cloneOptionsExcludingBlank('#ct-category', catSelect);
        ensureCurrentOptionSelected(catSelect, ticket.categoryId, ticket.category);

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
            opt.textContent = (label || 'Unknown') + ' (Inactive)';
            selectEl.appendChild(opt);
            selectEl.value = id;
        }
    }

    function loadEditSubcategories(categoryId, selectedId) {
        var subcatSelect = document.getElementById('td-edit-subcategory');
        if (!categoryId) {
            subcatSelect.innerHTML = '<option value="">Select a category first</option>';
            subcatSelect.disabled = true;
            return;
        }
        subcatSelect.disabled = true;
        subcatSelect.innerHTML = '<option value="">Loading…</option>';

        fetch('/api/subcategories?categoryId=' + encodeURIComponent(categoryId))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(function (subcategories) {
                var html = '<option value="">Optional</option>';
                subcategories.forEach(function (sc) {
                    if (!sc.active && String(sc.id) !== String(selectedId)) {
                        return;
                    }
                    html += '<option value="' + sc.id + '">' + escapeHtml(sc.name) + (sc.active ? '' : ' (Inactive)') + '</option>';
                });
                subcatSelect.innerHTML = html;
                if (selectedId) {
                    subcatSelect.value = selectedId;
                }
                subcatSelect.disabled = false;
            })
            .catch(function () {
                subcatSelect.innerHTML = '<option value="">Failed to load subcategories</option>';
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
            toggle.title = collapsed ? 'Hide' : 'Show';
            toggle.setAttribute('aria-expanded', collapsed ? 'true' : 'false');
        });
    }

    function actionAreaHtml(ticket) {
        var csrfInput = document.querySelector('#create-ticket-modal input[name="_csrf"]');
        var csrfToken = csrfInput ? csrfInput.value : '';
        var isOpen = ticket.status === 'OPEN';

        var html = '<div class="ticket-action-area">';
        html += '<form id="ticket-action-form" class="ticket-action-form" method="post" action="/tickets/' + ticket.id + '/comments">';
        html += '<input type="hidden" name="_csrf" value="' + escapeHtml(csrfToken) + '">';
        html += '<div class="form-group">';
        html += '<label for="ticket-action-comment">Update</label>';
        html += '<textarea id="ticket-action-comment" name="commentText" placeholder="Add a comment…" required="required"></textarea>';
        html += '</div>';

        if (isOpen) {
            html += '<div class="ticket-action-buttons ticket-action-buttons-spread">';
            html += '<button type="submit" formaction="/tickets/' + ticket.id + '/cancel" class="btn btn-danger" id="ticket-cancel-btn">✗ Cancel</button>';
            html += '<button type="submit" formaction="/tickets/' + ticket.id + '/resolve" class="btn btn-success" id="ticket-resolve-btn">✓ Resolve</button>';
            html += '<button type="submit" class="btn" id="ticket-comment-btn">💬 Comment</button>';
            html += '</div>';
        } else {
            html += '<div class="ticket-action-buttons ticket-action-buttons-center">';
            html += '<button type="submit" formaction="/tickets/' + ticket.id + '/reopen" class="btn btn-primary" id="ticket-reopen-btn">↺ Reopen</button>';
            html += '</div>';
        }

        html += '</form>';
        html += '</div>';
        return html;
    }

    function wireActionConfirms(ticket) {
        var isOpen = ticket.status === 'OPEN';
        var form = document.getElementById('ticket-action-form');
        var textarea = document.getElementById('ticket-action-comment');

        if (isOpen) {
            var commentBtn = document.getElementById('ticket-comment-btn');
            var resolveBtn = document.getElementById('ticket-resolve-btn');
            var cancelBtn = document.getElementById('ticket-cancel-btn');
            var buttons = [commentBtn, resolveBtn, cancelBtn];

            var placeholders = {
                comment: 'Add a comment…',
                resolve: 'Describe the solution…',
                cancel: 'Describe the reason for cancelling…'
            };

            commentBtn.addEventListener('mouseenter', function () { textarea.placeholder = placeholders.comment; });
            commentBtn.addEventListener('focus', function () { textarea.placeholder = placeholders.comment; });
            resolveBtn.addEventListener('mouseenter', function () { textarea.placeholder = placeholders.resolve; });
            resolveBtn.addEventListener('focus', function () { textarea.placeholder = placeholders.resolve; });
            cancelBtn.addEventListener('mouseenter', function () { textarea.placeholder = placeholders.cancel; });
            cancelBtn.addEventListener('focus', function () { textarea.placeholder = placeholders.cancel; });

            resolveBtn.addEventListener('click', function (e) {
                e.preventDefault();
                window.showConfirm('Resolve this ticket?').then(function (ok) {
                    if (ok) {
                        form.requestSubmit(resolveBtn);
                    }
                });
            });

            cancelBtn.addEventListener('click', function (e) {
                e.preventDefault();
                window.showConfirm('Cancel this ticket?').then(function (ok) {
                    if (ok) {
                        form.requestSubmit(cancelBtn);
                    }
                });
            });

            form.addEventListener('submit', function () {
                buttons.forEach(function (btn) {
                    btn.disabled = true;
                });
            });
        } else {
            var reopenBtn = document.getElementById('ticket-reopen-btn');

            reopenBtn.addEventListener('mouseenter', function () { textarea.placeholder = 'Describe why you are reopening…'; });
            reopenBtn.addEventListener('focus', function () { textarea.placeholder = 'Describe why you are reopening…'; });

            reopenBtn.addEventListener('click', function (e) {
                e.preventDefault();
                window.showConfirm('Reopen this ticket?').then(function (ok) {
                    if (ok) {
                        form.requestSubmit(reopenBtn);
                    }
                });
            });

            form.addEventListener('submit', function () {
                reopenBtn.disabled = true;
            });
        }
    }

    function historyItemHtml(h, ticketId) {
        var performer = escapeHtml(h.performedByUsername || 'A deleted user');
        var assignee = escapeHtml(h.assignedToUsername || 'a deleted user');
        var dotClass = '';
        var text;

        switch (h.action) {
            case 'CREATED':
                text = 'Created by ' + performer;
                break;
            case 'ASSIGNED':
                text = performer + ' Assigned ticket to ' + assignee + (h.commentText ? ' and commented:' : '.');
                break;
            case 'REASSIGNED':
                text = performer + ' Reassigned ticket to ' + assignee + (h.commentText ? ' and commented:' : '.');
                break;
            case 'COMMENT_ADDED':
                text = performer + ' Commented:';
                dotClass = 'dot-comment';
                break;
            case 'RESOLVED':
                text = performer + ' Resolved ticket' + (h.commentText ? ' with solution:' : '.');
                dotClass = 'dot-resolved';
                break;
            case 'CANCELLED':
                text = performer + ' Cancelled ticket' + (h.commentText ? ' with reason:' : '.');
                dotClass = 'dot-cancelled';
                break;
            case 'REOPENED':
                text = performer + ' Reopened ticket' + (h.commentText ? ' with reason:' : '.');
                break;
            default:
                text = performer + ' updated the ticket.';
        }

        var itemHtml = '<div class="timeline-item">';
        itemHtml += '<div class="timeline-dot ' + dotClass + '"></div>';
        itemHtml += '<p class="timeline-text">' + text + '</p>';
        if (h.commentText) {
            var canEditComment = h.commentId && currentUserId && String(h.performedById) === currentUserId;
            itemHtml += '<div class="timeline-quote-wrap" id="quote-view-' + h.commentId + '">';
            itemHtml += '<p class="timeline-quote">“' + escapeHtml(h.commentText) + '”</p>';
            if (canEditComment) {
                itemHtml += '<button type="button" class="icon-btn comment-edit-toggle" data-comment-id="' + h.commentId + '" title="Edit comment">✎</button>';
            }
            itemHtml += '</div>';
            if (canEditComment) {
                var csrfInputForComment = document.querySelector('#create-ticket-modal input[name="_csrf"]');
                var commentCsrfToken = csrfInputForComment ? csrfInputForComment.value : '';
                itemHtml += '<form id="comment-edit-' + h.commentId + '" class="comment-edit-form" method="post" ' +
                    'action="/tickets/' + ticketId + '/comments/' + h.commentId + '/edit" style="display:none;">';
                itemHtml += '<input type="hidden" name="_csrf" value="' + escapeHtml(commentCsrfToken) + '">';
                itemHtml += '<textarea name="text" required="required">' + escapeHtml(h.commentText) + '</textarea>';
                itemHtml += '<div class="ticket-action-buttons">';
                itemHtml += '<button type="button" class="btn btn-sm comment-edit-cancel" data-comment-id="' + h.commentId + '">Cancel</button>';
                itemHtml += '<button type="submit" class="btn btn-primary btn-sm">Save</button>';
                itemHtml += '</div>';
                itemHtml += '</form>';
            }
        }
        itemHtml += '<p class="timeline-time">' + formatDate(h.timestamp) + '</p>';
        itemHtml += '</div>';
        return itemHtml;
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
            return 'an unknown time ago';
        }
        var then = new Date(isoString).getTime();
        if (isNaN(then)) {
            return 'an unknown time ago';
        }
        var seconds = Math.floor((Date.now() - then) / 1000);
        if (seconds < 60) {
            return 'just now';
        }
        var units = [
            ['year', 31536000],
            ['month', 2592000],
            ['week', 604800],
            ['day', 86400],
            ['hour', 3600],
            ['minute', 60]
        ];
        for (var i = 0; i < units.length; i++) {
            var value = Math.floor(seconds / units[i][1]);
            if (value >= 1) {
                return value + ' ' + units[i][0] + (value === 1 ? '' : 's') + ' ago';
            }
        }
        return 'just now';
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
            subcategorySelect.innerHTML = '<option value="">Select a category first</option>';
            subcategorySelect.disabled = true;
            return;
        }

        subcategorySelect.disabled = true;
        subcategorySelect.innerHTML = '<option value="">Loading…</option>';

        fetch('/api/subcategories?categoryId=' + encodeURIComponent(categoryId))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(function (subcategories) {
                var html = '<option value="">Optional</option>';
                subcategories.forEach(function (sc) {
                    if (!sc.active && String(sc.id) !== String(selectedValue)) {
                        return;
                    }
                    html += '<option value="' + sc.id + '">' + escapeSubcategoryHtml(sc.name) + (sc.active ? '' : ' (Inactive)') + '</option>';
                });
                subcategorySelect.innerHTML = html;
                if (selectedValue) {
                    subcategorySelect.value = selectedValue;
                }
                subcategorySelect.disabled = false;
            })
            .catch(function () {
                subcategorySelect.innerHTML = '<option value="">Failed to load subcategories</option>';
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
