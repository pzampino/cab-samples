import org.apache.knox.gateway.shell.KnoxSession
import org.apache.knox.gateway.shell.idbroker.Credentials as CAB
import org.apache.knox.gateway.shell.Credentials as Credentials
import org.apache.knox.gateway.shell.knox.token.Token
import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils

////////
// This script demonstrates the acquisition of a Knox delegation token, which
// is subsequently used to acquire GCP storage credentials for the associated
// authenticated user.
//
// These storage credentials are then used to access the Google storage API.
////////

gateway_host = "localhost"
gateway = "https://" + gateway_host + ":8443/gateway/gcp-cab"

credentials = new Credentials()
credentials.add("ClearInput", "Enter username: ", "user")
           .add("HiddenInput", "Enter password: ", "pass")
credentials.collect()

tokenSession = KnoxSession.login("https://" + gateway_host + ":8443/gateway/dt",
                                 credentials.get("user").string(),
				 credentials.get("pass").string())
tokenResponse = Token.get(tokenSession).now().string
json = (new JsonSlurper()).parseText( tokenResponse )
accessToken = json.access_token
println "KnoxToken: " + accessToken
tokenSession.shutdown()

// Use the access token to get the cloud credentials from the ID Broker
headers = new HashMap<String, String>();
headers.put("Authorization", "Bearer " + accessToken)
session = KnoxSession.login(gateway, headers)
idBrokerResponse = CAB.get(session).now().string
println "Temp Credentials from ID Broker:\n" + idBrokerResponse

json = (new JsonSlurper()).parseText(idBrokerResponse)
storageAuthToken = json.accessToken
//println "Storage auth token: " + storageAuthToken + "\n"

session.shutdown()

// Use the temporary credentials to interact with Google storage
gcp = "https://www.googleapis.com"

httpClient = HttpClientBuilder.create().build()
getRequest = new HttpGet(gcp + "/storage/v1/b/?project=gcpidbroker")

// First WITHOUT the auth token from the ID Broker (should yield a 401)
response = httpClient.execute(getRequest)
println "Without Temp Credentials:\n" + EntityUtils.toString(response.getEntity())

// Then, WITH the auth token from the ID Broker (should succeed)
getRequest.setHeader("Authorization", "Bearer " + storageAuthToken)
response = httpClient.execute(getRequest)
println "With Temp Credentials:\n" + EntityUtils.toString(response.getEntity())

