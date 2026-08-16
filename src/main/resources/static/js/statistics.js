// Statistics page: a multi-line chart of tickets created/resolved/cancelled per
// day or month, plus two ranked horizontal-bar charts for the percentage share
// of tickets per category and per subcategory. Hand-rolled SVG — no chart
// library — colored via CSS custom properties (see app.css) so a theme toggle
// repaints the charts for free, without any JS re-render.
document.addEventListener('DOMContentLoaded', function () {
    var ticketsWrap = document.getElementById('tickets-chart-wrap');
    if (!ticketsWrap) {
        return;
    }

    var SVG_NS = 'http://www.w3.org/2000/svg';
    var CHART_WIDTH = 900;
    var SERIES = [
        { key: 'created', label: 'Δημιουργήθηκαν', cls: 'chart-series-created' },
        { key: 'cancelled', label: 'Ακυρώθηκαν', cls: 'chart-series-cancelled' },
        { key: 'resolved', label: 'Επιλύθηκαν', cls: 'chart-series-resolved' }
    ];
    var MONTH_NAMES = ['Ιαν', 'Φεβ', 'Μαρ', 'Απρ', 'Μαϊ', 'Ιουν', 'Ιουλ', 'Αυγ', 'Σεπ', 'Οκτ', 'Νοε', 'Δεκ'];

    wireTabs();
    renderLegend();
    wireGranularityToggle();
    loadTicketStats('daily');
    loadCategoryShares(document.getElementById('categories-chart-wrap'));

    function wireTabs() {
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
    }

    function renderLegend() {
        var legend = document.getElementById('tickets-chart-legend');
        SERIES.forEach(function (s) {
            var item = document.createElement('span');
            item.className = 'chart-legend-item';

            var key = document.createElement('span');
            key.className = 'chart-legend-key ' + s.cls;

            var text = document.createElement('span');
            text.textContent = s.label;

            item.appendChild(key);
            item.appendChild(text);
            legend.appendChild(item);
        });
    }

    function wireGranularityToggle() {
        document.querySelectorAll('.stats-granularity-btn').forEach(function (btn) {
            btn.addEventListener('click', function () {
                document.querySelectorAll('.stats-granularity-btn').forEach(function (b) {
                    b.classList.toggle('active', b === btn);
                });
                loadTicketStats(btn.getAttribute('data-granularity'));
            });
        });
    }

    function loadTicketStats(granularity) {
        ticketsWrap.innerHTML = '<p class="field-label chart-status">Φόρτωση…</p>';
        fetch('/api/statistics/tickets?granularity=' + encodeURIComponent(granularity))
            .then(function (res) {
                if (!res.ok) {
                    throw new Error('HTTP ' + res.status);
                }
                return res.json();
            })
            .then(function (points) {
                renderTimeSeriesChart(ticketsWrap, points, granularity);
            })
            .catch(function () {
                ticketsWrap.innerHTML = '<p class="field-label chart-status">Αποτυχία φόρτωσης στατιστικών.</p>';
            });
    }

    function loadCategoryShares(wrap) {
        if (!wrap) {
            return;
        }
        wrap.innerHTML = '<p class="field-label chart-status">Φόρτωση…</p>';
        Promise.all([fetchJson('/api/statistics/categories'), fetchJson('/api/statistics/subcategories')])
            .then(function (results) {
                var categories = results[0];
                var subcategoriesByParent = {};
                results[1].forEach(function (sub) {
                    if (!subcategoriesByParent[sub.parentId]) {
                        subcategoriesByParent[sub.parentId] = [];
                    }
                    subcategoriesByParent[sub.parentId].push(sub);
                });
                renderCategoryShareList(wrap, categories, subcategoriesByParent);
            })
            .catch(function () {
                wrap.innerHTML = '<p class="field-label chart-status">Αποτυχία φόρτωσης στατιστικών.</p>';
            });
    }

    function fetchJson(url) {
        return fetch(url).then(function (res) {
            if (!res.ok) {
                throw new Error('HTTP ' + res.status);
            }
            return res.json();
        });
    }

    // ---------------------------------------------------------------------
    // Time series line chart
    // ---------------------------------------------------------------------

    function renderTimeSeriesChart(wrap, points, granularity) {
        wrap.innerHTML = '';
        var hasData = points.length && points.some(function (p) {
            return p.created > 0 || p.resolved > 0 || p.cancelled > 0;
        });
        if (!hasData) {
            wrap.innerHTML = '<p class="field-label chart-status">Δεν υπάρχουν δεδομένα για αυτό το διάστημα.</p>';
            return;
        }

        var width = CHART_WIDTH, height = 320;
        var padLeft = 44, padRight = 16, padTop = 16, padBottom = 32;
        var innerWidth = width - padLeft - padRight;
        var innerHeight = height - padTop - padBottom;

        var maxRaw = 0;
        points.forEach(function (p) {
            maxRaw = Math.max(maxRaw, p.created, p.resolved, p.cancelled);
        });
        var ticks = niceTicks(maxRaw, 5);

        function xAt(i) {
            return points.length === 1 ? padLeft + innerWidth / 2 : padLeft + (i / (points.length - 1)) * innerWidth;
        }
        function yAt(v) {
            return padTop + innerHeight - (v / ticks.max) * innerHeight;
        }

        var svg = svgEl('svg', { class: 'chart-svg', viewBox: '0 0 ' + width + ' ' + height });

        var stepCount = Math.round(ticks.max / ticks.step);
        for (var g = 0; g <= stepCount; g++) {
            var v = g * ticks.step;
            var y = yAt(v);
            svg.appendChild(svgEl('line', { x1: padLeft, x2: width - padRight, y1: y, y2: y, class: 'chart-gridline' }));
            var gLabel = svgEl('text', { x: padLeft - 8, y: y + 4, class: 'chart-axis-label', 'text-anchor': 'end' });
            gLabel.textContent = String(v);
            svg.appendChild(gLabel);
        }

        svg.appendChild(svgEl('line', {
            x1: padLeft, x2: width - padRight, y1: padTop + innerHeight, y2: padTop + innerHeight, class: 'chart-axis-line'
        }));

        var maxLabels = granularity === 'monthly' ? 12 : 8;
        var labelEvery = Math.max(1, Math.ceil(points.length / maxLabels));
        labelIndices(points.length, labelEvery).forEach(function (i) {
            var xLabel = svgEl('text', { x: xAt(i), y: height - padBottom + 18, class: 'chart-axis-label', 'text-anchor': 'middle' });
            xLabel.textContent = formatPeriodLabel(points[i].period, granularity, false);
            svg.appendChild(xLabel);
        });

        SERIES.forEach(function (s) {
            var d = points.map(function (p, i) {
                return (i === 0 ? 'M' : 'L') + xAt(i) + ',' + yAt(p[s.key]);
            }).join(' ');
            svg.appendChild(svgEl('path', { d: d, class: 'chart-line ' + s.cls, stroke: 'currentColor' }));

            var lastIdx = points.length - 1;
            svg.appendChild(svgEl('circle', {
                cx: xAt(lastIdx), cy: yAt(points[lastIdx][s.key]), r: 4,
                class: 'chart-dot ' + s.cls, fill: 'currentColor'
            }));
        });

        var crosshair = svgEl('line', { x1: 0, x2: 0, y1: padTop, y2: padTop + innerHeight, class: 'chart-crosshair' });
        crosshair.style.display = 'none';
        svg.appendChild(crosshair);

        var hit = svgEl('rect', { x: padLeft, y: padTop, width: innerWidth, height: innerHeight, class: 'chart-hit-layer' });
        svg.appendChild(hit);

        wrap.appendChild(svg);

        var tooltip = document.createElement('div');
        tooltip.className = 'chart-tooltip';
        wrap.appendChild(tooltip);

        hit.addEventListener('pointermove', function (e) {
            var rect = svg.getBoundingClientRect();
            var scale = width / rect.width;
            var svgX = (e.clientX - rect.left) * scale;
            var idx = Math.round(((svgX - padLeft) / innerWidth) * (points.length - 1));
            idx = Math.max(0, Math.min(points.length - 1, idx));

            var px = xAt(idx);
            crosshair.setAttribute('x1', px);
            crosshair.setAttribute('x2', px);
            crosshair.style.display = '';

            showTimeSeriesTooltip(tooltip, wrap, svg, points[idx], px, granularity);
        });
        hit.addEventListener('pointerleave', function () {
            crosshair.style.display = 'none';
            tooltip.style.display = 'none';
        });
    }

    function showTimeSeriesTooltip(tooltip, wrap, svg, point, svgX, granularity) {
        tooltip.innerHTML = '';

        var title = document.createElement('div');
        title.className = 'chart-tooltip-title';
        title.textContent = formatPeriodLabel(point.period, granularity, true);
        tooltip.appendChild(title);

        SERIES.forEach(function (s) {
            var row = document.createElement('div');
            row.className = 'chart-tooltip-row';

            var keyWrap = document.createElement('span');
            keyWrap.className = 'chart-tooltip-row-key';
            var key = document.createElement('span');
            key.className = 'chart-tooltip-line-key ' + s.cls;
            var label = document.createElement('span');
            label.textContent = s.label;
            keyWrap.appendChild(key);
            keyWrap.appendChild(label);

            var value = document.createElement('span');
            value.className = 'chart-tooltip-value';
            value.textContent = String(point[s.key]);

            row.appendChild(keyWrap);
            row.appendChild(value);
            tooltip.appendChild(row);
        });

        tooltip.style.display = 'block';
        positionTooltip(tooltip, wrap, svg, svgX, 8, CHART_WIDTH);
    }

    // ---------------------------------------------------------------------
    // Category share list — each category is a <details> row; opening it
    // reveals that category's own subcategory breakdown (share is relative to
    // the category's total, not the grand total, so a group's bars sum to 100%).
    // ---------------------------------------------------------------------

    function renderCategoryShareList(wrap, categories, subcategoriesByParent) {
        wrap.innerHTML = '';
        if (!categories.length) {
            wrap.innerHTML = '<p class="field-label chart-status">Δεν υπάρχουν δεδομένα.</p>';
            return;
        }

        var maxPct = 0;
        categories.forEach(function (c) {
            maxPct = Math.max(maxPct, c.percentage);
        });

        var list = document.createElement('div');
        list.className = 'share-list';

        var tooltip = document.createElement('div');
        tooltip.className = 'chart-tooltip';

        categories.forEach(function (category) {
            var group = document.createElement('details');
            group.className = 'share-row-group';

            var summary = document.createElement('summary');
            var barWidth = maxPct > 0 ? Math.max((category.percentage / maxPct) * 100, 1) : 1;
            summary.appendChild(shareRowContent('▸', category.name, category.percentage.toFixed(1) + '%', barWidth));
            wireShareRowTooltip(summary, tooltip, wrap, category);
            group.appendChild(summary);

            var subItems = subcategoriesByParent[category.id] || [];
            var subContainer = document.createElement('div');
            subContainer.className = 'share-subrows';
            if (!subItems.length) {
                subContainer.className = 'share-row-empty';
                subContainer.textContent = 'Καμία υποκατηγορία με tickets.';
            } else {
                var maxSubPct = 0;
                subItems.forEach(function (s) {
                    maxSubPct = Math.max(maxSubPct, s.percentage);
                });
                subItems.forEach(function (sub) {
                    var subRow = document.createElement('div');
                    subRow.className = 'share-subrow';
                    var subBarWidth = maxSubPct > 0 ? Math.max((sub.percentage / maxSubPct) * 100, 1) : 1;
                    subRow.appendChild(shareRowContent(null, sub.name, sub.percentage.toFixed(1) + '%', subBarWidth));
                    wireShareRowTooltip(subRow, tooltip, wrap, sub);
                    subContainer.appendChild(subRow);
                });
            }
            group.appendChild(subContainer);

            list.appendChild(group);
        });

        wrap.appendChild(list);
        wrap.appendChild(tooltip);
    }

    // Builds one row's content (toggle arrow, name, track+bar, value) — shared
    // between the <summary> (category) and plain-div (subcategory) rows.
    function shareRowContent(toggleGlyph, name, valueText, barWidthPct) {
        var frag = document.createDocumentFragment();

        if (toggleGlyph !== null) {
            var toggle = document.createElement('span');
            toggle.className = 'share-row-toggle';
            toggle.textContent = toggleGlyph;
            frag.appendChild(toggle);
        }

        var nameEl = document.createElement('span');
        nameEl.className = 'share-row-name';
        nameEl.textContent = name;
        frag.appendChild(nameEl);

        var track = document.createElement('span');
        track.className = 'share-row-track';
        var bar = document.createElement('span');
        bar.className = 'share-row-bar';
        bar.style.width = barWidthPct + '%';
        track.appendChild(bar);
        frag.appendChild(track);

        var value = document.createElement('span');
        value.className = 'share-row-value';
        value.textContent = valueText;
        frag.appendChild(value);

        return frag;
    }

    function wireShareRowTooltip(rowEl, tooltip, wrap, item) {
        rowEl.addEventListener('pointerenter', function () {
            showShareRowTooltip(tooltip, wrap, rowEl, item);
        });
        rowEl.addEventListener('pointerleave', function () {
            tooltip.style.display = 'none';
        });
    }

    function showShareRowTooltip(tooltip, wrap, rowEl, item) {
        tooltip.innerHTML = '';

        var title = document.createElement('div');
        title.className = 'chart-tooltip-title';
        title.textContent = item.name;
        tooltip.appendChild(title);

        var row = document.createElement('div');
        row.className = 'chart-tooltip-row';
        var label = document.createElement('span');
        label.textContent = 'Tickets';
        var value = document.createElement('span');
        value.className = 'chart-tooltip-value';
        value.textContent = item.ticketCount + ' (' + item.percentage.toFixed(1) + '%)';
        row.appendChild(label);
        row.appendChild(value);
        tooltip.appendChild(row);

        tooltip.style.display = 'block';
        var wrapRect = wrap.getBoundingClientRect();
        var rowRect = rowEl.getBoundingClientRect();
        tooltip.style.transform = '';
        tooltip.style.left = (rowRect.left - wrapRect.left) + 'px';
        tooltip.style.top = (rowRect.bottom - wrapRect.top + 4) + 'px';
    }

    // ---------------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------------

    function positionTooltip(tooltip, wrap, svg, svgX, svgY, viewWidth, viewHeight) {
        var wrapRect = wrap.getBoundingClientRect();
        var svgRect = svg.getBoundingClientRect();
        var scaleX = svgRect.width / viewWidth;
        var scaleY = viewHeight ? svgRect.height / viewHeight : scaleX;

        var left = svgX * scaleX + (svgRect.left - wrapRect.left) + 12;
        if (left + 180 > wrapRect.width) {
            left = svgX * scaleX + (svgRect.left - wrapRect.left) - 12;
            tooltip.style.transform = 'translateX(-100%)';
        } else {
            tooltip.style.transform = '';
        }
        var top = svgY * scaleY + (svgRect.top - wrapRect.top);

        tooltip.style.left = left + 'px';
        tooltip.style.top = top + 'px';
    }

    // Regular-interval label indices, always ending on the last point — but if
    // the last regular tick would land too close to that forced final label,
    // it's replaced rather than added, so the two never collide (see
    // marks-and-anatomy.md: a label that won't fit doesn't get clipped or overlap).
    function labelIndices(count, every) {
        var indices = [];
        for (var i = 0; i < count; i += every) {
            indices.push(i);
        }
        var lastIdx = count - 1;
        var lastShown = indices[indices.length - 1];
        if (lastShown !== lastIdx) {
            if (lastIdx - lastShown < every / 2) {
                indices[indices.length - 1] = lastIdx;
            } else {
                indices.push(lastIdx);
            }
        }
        return indices;
    }

    function svgEl(tag, attrs) {
        var el = document.createElementNS(SVG_NS, tag);
        Object.keys(attrs).forEach(function (key) {
            el.setAttribute(key, attrs[key]);
        });
        return el;
    }

    function formatPeriodLabel(period, granularity, full) {
        if (granularity === 'monthly') {
            var parts = period.split('-');
            var monthName = MONTH_NAMES[parseInt(parts[1], 10) - 1];
            return full ? (monthName + ' ' + parts[0]) : monthName;
        }
        var d = new Date(period + 'T00:00:00');
        return full
            ? d.toLocaleDateString('el-GR', { day: 'numeric', month: 'short', year: 'numeric' })
            : d.toLocaleDateString('el-GR', { day: 'numeric', month: 'short' });
    }

    // Standard "nice numbers" tick algorithm (Heckbert) so the y-axis lands on
    // clean values (0/5/10/20…) instead of the raw data max.
    function niceTicks(maxValue, tickCount) {
        if (maxValue <= 0) {
            return { max: 4, step: 1 };
        }
        var range = niceNumber(maxValue, false);
        var step = niceNumber(range / (tickCount - 1), true);
        return { max: Math.ceil(maxValue / step) * step, step: step };
    }

    function niceNumber(value, round) {
        var exponent = Math.floor(Math.log(value) / Math.LN10);
        var fraction = value / Math.pow(10, exponent);
        var niceFraction;
        if (round) {
            if (fraction < 1.5) { niceFraction = 1; }
            else if (fraction < 3) { niceFraction = 2; }
            else if (fraction < 7) { niceFraction = 5; }
            else { niceFraction = 10; }
        } else {
            if (fraction <= 1) { niceFraction = 1; }
            else if (fraction <= 2) { niceFraction = 2; }
            else if (fraction <= 5) { niceFraction = 5; }
            else { niceFraction = 10; }
        }
        return niceFraction * Math.pow(10, exponent);
    }
});
