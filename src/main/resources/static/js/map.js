document.addEventListener('DOMContentLoaded', function () {
    var map = L.map('map').setView([53.55, 9.99], 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    var markers = {};

    var statusLabels = {
        1: 'Frei uber Funk',
        2: 'Freи auс Wache',
        3: 'Einsatz uebernommen',
        4: 'Am Einsatzort',
        6: 'Ausser Dienst'
    };

    var allStatusCodes = [1, 2, 3, 4, 6];

    function getCsrfToken() {
        var name = 'XSRF-TOKEN';
        var cookieValue = null;
        if (document.cookie && document.cookie !== '') {
            var cookies = document.cookie.split(';');
            for (var i = 0; i < cookies.length; i++) {
                var cookie = cookies[i].trim();
                if (cookie.substring(0, name.length + 1) === (name + '=')) {
                    cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                    break;
                }
            }
        }
        return cookieValue;
    }

    function getStatusColor(status) {
        if (status === 1 || status === 2) return '#2ecc71';
        if (status === 3 || status === 4) return '#e74c3c';
        if (status === 6) return '#95a5a6';
        return '#888888';
    }

    function formatUpdatedAt(dateStr) {
        var d = new Date(dateStr);
        var day = String(d.getDate()).padStart(2, '0');
        var month = String(d.getMonth() + 1).padStart(2, '0');
        var year = d.getFullYear();
        var hours = String(d.getHours()).padStart(2, '0');
        var minutes = String(d.getMinutes()).padStart(2, '0');
        return day + '.' + month + '.' + year + ' ' + hours + ':' + minutes;
    }

    function changeStatus(vehicleId, newStatus) {
        var csrfToken = getCsrfToken();
        fetch('/api/vehicles/' + vehicleId + '/status', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': csrfToken || ''
            },
            body: JSON.stringify({ status: newStatus })
        }).then(function (response) {
            if (!response.ok) {
                console.error('Status change failed with status:', response.status);
            }
        }).catch(function (err) {
            console.error('Status change failed:', err);
        });
    }

    function createPopupContent(vehicleId, vehicleStatus) {
        var html = '<b>' + vehicle.callsign + '</b><br/>';
        html += vehicle.type + '<br/>';
        html += '<i>Status:</i> ' + (statusLabels[vehicle.status] || vehicle.status) + '<br/>';
        html += '<i>Letzte Aktualisierung:</i> ' + formatUpdatedAt(vehicle.updatedAt);
        html += '<hr/>';

        for (var i = 0; i < allStatusCodes.length; i++) {
            var code = allStatusCodes[i];
            var activeClass = vehicle.status === code ? ' status-active' : '';
            html += '<button class="status-btn' + activeClass + '" data-vehicle-id="' + vehicleId + '" data-status="' + code + '">' + statusLabels[code] + '</button><br/>';
        }

        return html;
    }

    function updateMap(vehicles) {
        var currentIds = {};

        vehicles.forEach(function (v) {
            currentIds[v.id] = v;

            if (!markers[v.id]) {
                markers[v.id] = L.circleMarker([v.lat, v.lng], {
                    radius: 9,
                    color: getStatusColor(v.status),
                    fillColor: getStatusColor(v.status),
                    fillOpacity: 0.7
                });
                markers[v.id].bindPopup(createPopupContent(v));
                markers[v.id].addTo(map);
            } else {
                markers[v.id].setLatLng([v.lat, v.lng]);
                markers[v.id].setStyle({ color: getStatusColor(v.status), fillColor: getStatusColor(v.status) });
                markers[v.id].setPopupContent(createPopupContent(v));
            }
        });

        Object.keys(markers).forEach(function (id) {
            if (!currentIds[id]) {
                map.removeLayer(markers[id]);
                delete markers[id];
            }
        });

    function handleStatusClick(event) {
        var target = event.target;
        if (!(target.matches && !target.matches('.status-btn')) return;

        if (!target.classList.contains('status-btn')) return;

        var vehicleId = parseInt(target.getAttribute('data-vehicle-id'));
        var newStatus = parseInt(target.getAttribute('data-status'));

        changeStatus(vehicleId, newStatus);
    }

    document.addEventListener('click', handleStatusClick);


    function fetchVehicles() {
        fetch('/api/vehicles')
            .then(function (response) { return response.json(); })
            .then(updateMap)
            .catch(function (err) { console.error('Failed to fetch vehicles:', err); });
    }

    fetchVehicles();
    setInterval(fetchVehicles, 10000);
});
