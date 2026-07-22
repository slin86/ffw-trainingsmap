document.addEventListener('DOMContentLoaded', function () {
    var map = L.map('map').setView([53.55, 9.99], 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    var markers = {};

    var statusLabels = {
        1: 'Frei uber Funk',
        2: 'Frei auf Wache',
        3: 'Einsatz uebernommen',
        4: 'Am Einsatzort',
        6: 'Ausser Dienst'
    };

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

    function buildPopup(vehicle) {
        return '<b>' + vehicle.callsign + '</b><br/>' +
            vehicle.type + '<br/>' +
            '<i>Status:</i> ' + (statusLabels[vehicle.status] || vehicle.status) + '<br/>' +
            '<i>Letzte Aktualisierung:</i> ' + formatUpdatedAt(vehicle.updatedAt);
    }

    function updateMap(vehicles) {
        var currentIds = {};

        vehicles.forEach(function (v) {
            currentIds[v.id] = v;

            if (!markers[v.id]) {
                markers[v.id] = L.circleMarker([v.lat, v.lng], {
                    radius: 8,
                    color: getStatusColor(v.status),
                    fillColor: getStatusColor(v.status),
                    fillOpacity: 0.7
                });
                markers[v.id].bindPopup(buildPopup(v));
                markers[v.id].addTo(map);
            } else {
                markers[v.id].setLatLng([v.lat, v.lng]);
                markers[v.id].setStyle({ color: getStatusColor(v.status), fillColor: getStatusColor(v.status) });
                markers[v.id].setPopupContent(buildPopup(v));
            }
        });

        Object.keys(markers).forEach(function (id) {
            if (!currentIds[id]) {
                map.removeLayer(markers[id]);
                delete markers[id];
            }
        });
    }

    function fetchVehicles() {
        fetch('/api/vehicles')
            .then(function (response) { return response.json(); })
            .then(updateMap)
            .catch(function (err) { console.error('Failed to fetch vehicles:', err); });
    }

    fetchVehicles();
    setInterval(fetchVehicles, 10000);
});
