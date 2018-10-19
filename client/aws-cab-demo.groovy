import org.apache.knox.gateway.shell.Hadoop
import org.apache.knox.gateway.shell.idbroker.Credentials as CAB
import org.apache.knox.gateway.shell.Credentials as Credentials
import org.apache.knox.gateway.shell.knox.token.Token
import com.jayway.jsonpath.JsonPath
import groovy.json.JsonSlurper

////////
// This script demonstrates the acquisition of a Knox delegation token, which
// is subsequently used to acquire AWS storage credentials for the associated
// authenticated user.
////////

gateway = "https://localhost:8443/gateway/cloud"

credentials = new Credentials()
credentials.add("ClearInput", "Enter username: ", "user")
           .add("HiddenInput", "Enter password: ", "pass")
credentials.collect()

// Get a delegation token from Knox
dtSession = Hadoop.login("https://localhost:8443/gateway/dt",
                         credentials.get("user").string(),
                         credentials.get("pass").string())
dtResponse = Token.get(dtSession).now().string
json = (new JsonSlurper()).parseText( dtResponse )
delegationToken = json.access_token
println "Delegation Token: " + delegationToken
dtSession.shutdown()

// Use the delegation token to get the cloud credentials from the ID Broker
headers = new HashMap<String, String>();
headers.put("Authorization", "Bearer " + delegationToken)
session = Hadoop.login(gateway, headers)
println "Cloud Credentials: " + CAB.get(session).now().string

session.shutdown()

