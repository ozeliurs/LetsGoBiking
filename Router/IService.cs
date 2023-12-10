using Apache.NMS;
using Apache.NMS.ActiveMQ;
using GoogleApi.Entities.Maps.Directions.Response;
using Newtonsoft.Json;

namespace Router
{
    [ServiceContract(Namespace = "http://wsdl.letsgobiking.com")]
    public interface IService
    {
        [OperationContract]
        List<Route> GetRoute(Coordinate origin, Coordinate destination);
        
        [OperationContract]
        List<Coordinate> Geocode(string address);
    }

    public class Service : IService
    {
        private readonly HttpClient _httpClient = new HttpClient();
        
        private static double WALK_SPEED = 1.4; // m/s
        private static double BIKE_SPEED = 4.1; // m/s

        //private readonly string _jcDecauxBaseUrl = "https://api.jcdecaux.com/vls/v3";
        private readonly string _jcDecauxBaseUrl = "http://localhost:5000/jcdecaux";
        private readonly string _jcDecauxApiKey = "579b59e06dc51aa8190ad83e4511632132a09b55";
        private readonly List<Station> _jcDecauxStations = new List<Station>();

        private readonly string _openRouteServiceBaseUrl = "https://api.openrouteservice.org";
        private readonly string _openRouteServiceApiKey = "5b3ce3597851110001cf6248ba0ea999ab9e47e39f4ae0415f4840e3";

        public List<Coordinate> Geocode(string address)
        {
            string url =
                $"{_openRouteServiceBaseUrl}/geocode/search?api_key={_openRouteServiceApiKey}&text={address.Replace(" ", "%20")}";

            var request = new HttpRequestMessage(HttpMethod.Get, url);
            HttpResponseMessage response = _httpClient.SendAsync(request).Result;
            
            dynamic json = JsonConvert.DeserializeObject(response.Content.ReadAsStringAsync().Result);

            List<Coordinate> coordinates = new List<Coordinate>();

            foreach (var feature in json.features)
            {
                coordinates.Add(new Coordinate
                {
                    Latitude = feature.geometry.coordinates[1],
                    Longitude = feature.geometry.coordinates[0]
                });
            }

            return coordinates;
        }

        private static double ToRadians(double angle)
        {
            return Math.PI * angle / 180.0;
        }

        private int GetDistance(Coordinate origin, Coordinate destination)
        {
            double dLat = ToRadians(destination.Latitude - origin.Latitude);
            double dLon = ToRadians(destination.Longitude - origin.Longitude);
            double a = Math.Sin(dLat / 2) * Math.Sin(dLat / 2) +
                       Math.Cos(ToRadians(origin.Latitude)) * Math.Cos(ToRadians(destination.Latitude)) *
                       Math.Sin(dLon / 2) * Math.Sin(dLon / 2);
            double c = 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));
            return (int)(6371 * c * 1000); // convert to meters
        }

        private int GetDistance(Route route)
        {
            int distance = 0;
            
            for (int i = 0; i < route.coordinates.Count - 1; i++)
            {
                distance += GetDistance(route.coordinates[i], route.coordinates[i + 1]);
            }
            
            return distance;
        }

        private Route GetRoute(Coordinate origin, Coordinate destination, string travelMode)
        {
            string url =
                $"{_openRouteServiceBaseUrl}/v2/directions/{travelMode}?api_key={_openRouteServiceApiKey}&start={origin.Longitude.ToString().Replace(",", ".")},{origin.Latitude.ToString().Replace(",", ".")}&end={destination.Longitude.ToString().Replace(",", ".")},{destination.Latitude.ToString().Replace(",", ".")}";

            var request = new HttpRequestMessage(HttpMethod.Get, url);
            HttpResponseMessage response = _httpClient.SendAsync(request).Result;

            dynamic json = JsonConvert.DeserializeObject(response.Content.ReadAsStringAsync().Result);

            Route route = new Route();

            route.travelMode = travelMode;
            route.coordinates = new List<Coordinate>();
            route.instructions = new List<string>();

            foreach (var feature in json.features)
            {
                foreach (var coordinate in feature.geometry.coordinates)
                {
                    route.coordinates.Add(new Coordinate
                    {
                        Latitude = coordinate[1],
                        Longitude = coordinate[0]
                    });
                }
            }
            
            foreach (var segment in json.features[0].properties.segments)
            {
                foreach (var step in segment.steps)
                {
                    route.instructions.Add((string)step.instruction);
                }
            }
            
            return route;
        }

        private List<Station> getClosestStations(Coordinate coordinate, int count = 1)
        {
            List<Station> stations = GetStations();

            List<Station> sortedStations =
                stations.OrderBy(station => GetDistance(coordinate, station.Address)).ToList();

            return sortedStations.GetRange(0, Math.Min(sortedStations.Count, count));
        }

        private List<Station> GetStations()
        {
            if (_jcDecauxStations.Count != 0)
            {
                return _jcDecauxStations;
            }

            string url = $"{_jcDecauxBaseUrl}/stations?apiKey={_jcDecauxApiKey}";

            var request = new HttpRequestMessage(HttpMethod.Get, url);
            HttpResponseMessage response = _httpClient.SendAsync(request).Result;

            dynamic json = JsonConvert.DeserializeObject(response.Content.ReadAsStringAsync().Result);
            
            if (json == null)
            {
                throw new Exception("No stations found"); 
            }

            foreach (var station in json)
            {
                _jcDecauxStations.Add(new Station
                {
                    Name = station.name,
                    Address = new Coordinate
                    {
                        Latitude = station.position.latitude,
                        Longitude = station.position.longitude
                    },
                    AvailableBikes = station.totalStands.availabilities.bikes,
                    AvailableBikeStands = station.totalStands.availabilities.stands
                });
            }

            return _jcDecauxStations;
        }

        public List<Route> GetRoute(Coordinate origin, Coordinate destination)
        {
            // == On Foot Route ==
            Route onFootRoute = GetRoute(origin, destination, TravelMode.FootWalking);

            // == Cycling Route ==
            // Closest station to origin
            List<Station> originStations = getClosestStations(origin, 5);

            Station closestOriginStation =
                originStations.OrderBy(station => GetDistance(origin, station.Address)).ToList()[0];

            // Closest station to destination
            List<Station> destinationStations = getClosestStations(destination, 5);
            
            Station closestDestinationStation =
                destinationStations.OrderBy(station => GetDistance(destination, station.Address)).ToList()[0];
            
            // Route from origin to closest station
            Route originToStationRoute = GetRoute(origin, closestOriginStation.Address, TravelMode.CyclingRegular);
            
            // Route from closest station to destination
            Route stationToDestinationRoute = GetRoute(closestDestinationStation.Address, destination, TravelMode.CyclingRegular);
            
            // Route from origin to destination
            Route cyclingRoute = GetRoute(closestOriginStation.Address, closestDestinationStation.Address, TravelMode.CyclingRegular);
            
            // Compare times
            double o_s = GetDistance(originToStationRoute) / WALK_SPEED;
            double s_s = GetDistance(stationToDestinationRoute) / WALK_SPEED;
            double s_d = GetDistance(cyclingRoute) / BIKE_SPEED;
            
            double cyclingTime = o_s + s_s + s_d;
            double walkingTime = GetDistance(onFootRoute) / WALK_SPEED;
            
            Console.WriteLine(walkingTime);
            Console.WriteLine(cyclingTime);
            
            if (walkingTime < cyclingTime)
            {
                return new List<Route> {onFootRoute};
            }
            
            return new List<Route>
            {
                originToStationRoute,
                cyclingRoute,
                stationToDestinationRoute
            };
        }
    }
    
    internal static class TravelMode
    {
        public static readonly string DrivingCar = "driving-car";
        public static readonly string DrivingHgv = "driving-hgv";
        public static readonly string CyclingRegular = "cycling-regular";
        public static readonly string CyclingRoad = "cycling-road";
        public static readonly string CyclingMountain = "cycling-mountain";
        public static readonly string CyclingElectric = "cycling-electric";
        public static readonly string FootWalking = "foot-walking";
        public static readonly string FootHiking = "foot-hiking";
        public static readonly string Wheelchair = "wheelchair";
    }

    internal class Station
    {
        public string Name { get; set; }
        public Coordinate Address { get; set; }
        public int AvailableBikes { get; set; }
        public int AvailableBikeStands { get; set; }
    }

    [DataContract]
    public class Route
    {
        [DataMember] public string travelMode { get; set; }
        [DataMember] public List<Coordinate> coordinates { get; set; }
        [DataMember] public List<string> instructions { get; set; }
    }
    
    [DataContract]
    public class Coordinate
    {
        [DataMember] public double Latitude { get; set; }
        [DataMember] public double Longitude { get; set; }
    }
}