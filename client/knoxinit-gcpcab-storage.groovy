import org.apache.knox.gateway.shell.Hadoop
import org.apache.knox.gateway.shell.KnoxTokenCredentialCollector
import org.apache.knox.gateway.shell.idbroker.Credentials as CAB
import org.apache.knox.gateway.shell.knox.token.Token
import groovy.json.JsonSlurper
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils

////////
// This script assumes that knoxinit has been invoked for the calling user,
// such that the access token is available in the user's home directory.
//
// This token is subsequently used to acquire GCP storage credentials for the
// associated authenticated user.
//
// These storage credentials are then used to access the Google storage API.
////////

gateway = "https://localhost:8443/gateway/gcp-cab"

// Get the delegation token from a previous knoxinit
credentials = new KnoxTokenCredentialCollector()
credentials.collect()
delegationToken = credentials.string()

println "Knox Token: " + delegationToken + "\n" // TODO: DELETE ME

// Use the access token to get the cloud credentials from the ID Broker
headers = new HashMap<String, String>();
headers.put("Authorization", "Bearer " + delegationToken)
session = Hadoop.login(gateway, headers)
cabResponse = CAB.get(session).now().string
println "Temp Credentials from CAB:\n" + cabResponse

json = (new JsonSlurper()).parseText(cabResponse)
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

