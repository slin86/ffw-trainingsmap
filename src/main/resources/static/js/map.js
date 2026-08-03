document.addEventListener('DOMContentLoaded', function () {
    var map = L.map('map').setView([53.55, 9.99], 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    var markers = {};
    var locationMarkers = {};
    var sidebarCollapsed = false;

    var statusLabels = {
        1: 'Frei über Funk',
        2: 'Frei auf Wache',
        3: 'Einsatz übernommen',
        4: 'Am Einsatzort',
        6: 'Außer Dienst'
    };

    var allStatusCodes = [1, 2, 3, 4, 6];

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

    function toggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
        var overlay = document.getElementById('sidebar');
        var btn = document.getElementById('toggleBtn');
        
        if (sidebarCollapsed) {
            overlay.classList.add('sidebar-collapsed');
            btn.textContent = '▲';
        } else {
            overlay.classList.remove('sidebar-collapsed');
            btn.textContent = '▼';
        }
    }

    function changeStatus(vehicleId, newStatus) {
        var token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        var header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
        var headers = { 'Content-Type': 'application/json' };
        headers[header] = token;

        fetch('/api/vehicles/' + vehicleId + '/status', {
            method: 'PUT',
            headers: headers,
            body: JSON.stringify({ status: newStatus })
        }).then(function (response) {
            if (!response.ok) {
                console.error('Status change failed with status:', response.status);
                return;
            }
            return response.json();
        }).then(function (updatedVehicle) {
            if (updatedVehicle && markers[vehicleId]) {
                markers[vehicleId].setStyle({
                    color: getStatusColor(updatedVehicle.status),
                    fillColor: getStatusColor(updatedVehicle.status)
                });
                var popupContent = createPopupContent(updatedVehicle);
                markers[vehicleId].setPopupContent(popupContent);
            }
        }).catch(function (err) {
            console.error('Status change failed:', err);
        });
    }

    function createVehiclePopupContent(vehicle) {
        var html = '<b>' + vehicle.callsign + '</b><br/>';
        html += vehicle.type + '<br/>';
        html += '<i>Status:</i> ' + (statusLabels[vehicle.status] || vehicle.status) + '<br/>';
        html += '<i>Letzte Aktualisierung:</i> ' + formatUpdatedAt(vehicle.updatedAt);
        html += '<hr/>';

        for (var i = 0; i < allStatusCodes.length; i++) {
            var code = allStatusCodes[i];
            var activeClass = vehicle.status === code ? ' status-active' : '';
            html += '<button class="status-btn' + activeClass + '" onclick="window.changeMarkerStatus(' + vehicle.id + ', ' + code + ');" data-vehicle-id="' + vehicle.id + '" data-status="' + code + '">' + statusLabels[code] + '</button><br/>';
        }

        return html;
    }

    function createLocationPopupContent(location) {
        var html = '<b>' + location.name + '</b><br/>';
        html += '<i>Typ:</i> ' + (location.location_type === 'STATION' ? 'Feuerwache' : 'Einsatzort') + '<br/>';
        
        if (location.vehicles && location.vehicles.length > 0) {
            html += '<hr/><b>Fahrzeuge:</b><br/>';
            location.vehicles.forEach(function (v) {
                var statusColor = getStatusColor(v.status);
                html += '<div style="margin:4px 0;">';
                html += '<span style="display:inline-block;width:12px;height:12px;background:' + statusColor + ';border-radius:50%;vertical-align:middle;margin-right:6px;"></span>';
                html += v.callsign + ' (' + (statusLabels[v.status] || v.status) + ')';
                html += '</div>';
            });
        } else {
            html += '<br/><i>Keine Fahrzeuge zugewiesen</i>';
        }

        return html;
    }

    window.changeMarkerStatus = function(vehicleId, newStatus) {
        changeStatus(vehicleId, newStatus);
    };

    function updateSidebar(vehicles, locations) {
        var vehicleList = document.getElementById('vehicle-list');
        var locationList = document.getElementById('location-list');

        // Format vehicles
        vehicleList.innerHTML = '';
        if (vehicles && vehicles.length > 0) {
            vehicles.forEach(function (v) {
                var wasAtLocation = v.location && v.location.id;
                var statusColor = getStatusColor(v.status);
                
                var item = document.createElement('div');
                item.className = 'list-item';
                item.innerHTML = '<div class="vehicle-info">' +
                    '<span style="color:' + statusColor + ';">●</span> ' +
                    '<strong>' + v.callsign + '</strong><br/>' +
                    '<span class="vehicle-status">' + (statusLabels[v.status] || v.status) + '</span>' +
                    (wasAtLocation ? '' : '<span class="underway">unterwegs</span>') +
                '</div>';
                
                item.onclick = function() {
                    if (markers[v.id]) {
                        markers[v.id].openPopup();
                        map.flyTo(markers[v.id].getLatLng(), 14);
                    }
                };
                vehicleList.appendChild(item);
            });
        } else {
            vehicleList.innerHTML = '<div class="list-item" style="text-align:center;color:#666;">Keine Fahrzeuge</div>';
        }

        // Format locations
        locationList.innerHTML = '';
        if (locations && locations.length > 0) {
            locations.forEach(function (l) {
                var isStation = l.location_type === 'STATION';
                var typeIconClass = isStation ? 'type-station' : 'type-incident';
                
                var item = document.createElement('div');
                item.className = 'list-item';
                item.innerHTML = '<div>' +
                    '<span class="location-type ' + typeIconClass + '"></span>' +
                    '<strong>' + l.name + '</strong><br/>' +
                    '<small>(' + (isStation ? 'Feuerwache' : 'Einsatzort') + ')' + '</small>' +
                '</div>';
                
                item.onclick = function() {
                    if (locationMarkers[l.id]) {
                        locationMarkers[l.id].openPopup();
                        map.flyTo(locationMarkers[l.id].getLatLng(), 14);
                    }
                };
                locationList.appendChild(item);
            });
        } else {
            locationList.innerHTML = '<div class="list-item" style="text-align:center;color:#666;">Keine Orte</div>';
        }
    }

    function updateMap(vehicles, locations) {
        var currentVehicleIds = {};
        var currentLocationIds = {};

        // Process vehicles
        vehicles.forEach(function (v) {
            currentVehicleIds[v.id] = v;

            // Determine lat/lng based on location_id
            var lat = v.lat;
            var lng = v.lng;
            if (v.location && v.location.id) {
                lat = v.location.lat;
                lng = v.location.lng;
            }

            if (!markers[v.id]) {
                markers[v.id] = L.circleMarker([lat, lng], {
                    radius: 11,
                    color: '#1a1a1a',
                    weight: 2,
                    fillColor: getStatusColor(v.status),
                    fillOpacity: 1.0
                });
                markers[v.id].bindPopup(createVehiclePopupContent(v));
                markers[v.id].addTo(map);
            } else {
                if (markers[v.id].getLatLng().lat !== lat || markers[v.id].getLatLng().lng !== lng) {
                    markers[v.id].setLatLng([lat, lng]);
                }
                markers[v.id].setStyle({ color: getStatusColor(v.status), fillColor: getStatusColor(v.status) });
                markers[v.id].setPopupContent(createVehiclePopupContent(v));
            }
        });

        // Remove old vehicle markers
        Object.keys(markers).forEach(function (id) {
            if (!currentVehicleIds[id]) {
                map.removeLayer(markers[id]);
                delete markers[id];
            }
        });

        // Process locations
        if (locations) {
            locations.forEach(function (loc) {
                currentLocationIds[loc.id] = loc;

                if (!locationMarkers[loc.id]) {
                    var isStation = loc.location_type === 'STATION';
                    locationMarkers[loc.id] = L.circleMarker([loc.lat, loc.lng], {
                        radius: 15,
                        color: isStation ? '#1976d2' : '#fbc02d',
                        weight: 3,
                        fillColor: isStation ? '#4fc3f7' : '#fff9c4',
                        fillOpacity: 0.8
                    });
                    locationMarkers[loc.id].bindPopup(createLocationPopupContent(loc));
                    locationMarkers[loc.id].addTo(map);
                } else {
                    if (locationMarkers[loc.id].getLatLng().lat !== loc.lat || locationMarkers[loc.id].getLatLng().lng !== loc.lng) {
                        locationMarkers[loc.id].setLatLng([loc.lat, loc.lng]);
                    }
                    locationMarkers[loc.id].setStyle({ color: isStation ? '#1976d2' : '#fbc02d', fillColor: isStation ? '#4fc3f7' : '#fff9c4' });
                    locationMarkers[loc.id].setPopupContent(createLocationPopupContent(loc));
                }
            });

            // Remove old location markers
            Object.keys(locationMarkers).forEach(function (id) {
                if (!currentLocationIds[id]) {
                    map.removeLayer(locationMarkers[id]);
                    delete locationMarkers[id];
                }
            });
        }

        // Update sidebar
        updateSidebar(vehicles, locations);
    }

    function fetchData() {
        var vehiclesPromise = fetch('/api/vehicles').then(function (response) { return response.json(); });
        var locationsPromise = fetch('/api/locations').then(function (response) { return response.json(); });

        Promise.all([vehiclesPromise, locationsPromise]).then(function (results) {
            updateMap(results[0], results[1]);
        }).catch(function (err) {
            console.error('Failed to fetch data:', err);
        });
    }

    fetchData();
    setInterval(fetchData, 10000);
});
