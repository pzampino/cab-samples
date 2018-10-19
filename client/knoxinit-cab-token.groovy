import org.apache.knox.gateway.shell.Hadoop
import org.apache.knox.gateway.shell.KnoxTokenCredentialCollector
import org.apache.knox.gateway.shell.idbroker.Credentials as CAB

////////
// This script assumes that knoxinit has been invoked for the calling user,
// such that the access token is available in the user's home directory.
//
// This token is subsequently used to acquire AWS storage credentials for the
// associated authenticated user.
////////

gateway = "https://localhost:8443/gateway/cloud"

// Get the access token from a previous knoxinit
credentials = new KnoxTokenCredentialCollector()
credentials.collect()
delegationToken = credentials.string()

// Use the delegation token to get the cloud credentials from the ID Broker
headers = new HashMap<String, String>();
headers.put("Authorization", "Bearer " + delegationToken)

session = Hadoop.login(gateway, headers)

println "Cloud Credentials: " + CAB.get(session).now().string

session.shutdown()

