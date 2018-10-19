import org.apache.knox.gateway.shell.Hadoop
import org.apache.knox.gateway.shell.idbroker.Credentials as CAB
import org.apache.knox.gateway.shell.Credentials as Credentials
import org.apache.knox.gateway.shell.knox.token.Token
import com.jayway.jsonpath.JsonPath
import groovy.json.JsonSlurper

gateway = "https://localhost:8443/gateway/gcp"

credentials = new Credentials()
credentials.add("ClearInput", "Enter username: ", "user")
           .add("HiddenInput", "Enter password: ", "pass")
credentials.collect()

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

