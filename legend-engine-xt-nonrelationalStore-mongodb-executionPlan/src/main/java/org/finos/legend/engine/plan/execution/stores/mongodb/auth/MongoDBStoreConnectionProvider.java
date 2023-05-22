// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.plan.execution.stores.mongodb.auth;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.connection.ConnectionProvider;
import org.finos.legend.connection.ConnectionSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.KerberosUtils;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;

import java.security.PrivilegedAction;
import java.util.List;
import java.util.function.Supplier;

public class MongoDBStoreConnectionProvider extends ConnectionProvider<Supplier<MongoClient>>
{

    private static final String ADMIN_DB = "admin";

    public MongoDBStoreConnectionProvider(CredentialProviderProvider credentialProviderProvider)
    {
        super(credentialProviderProvider);
    }

    @Override
    public Supplier<MongoClient> makeConnection(ConnectionSpecification connectionSpec, AuthenticationSpecification authenticationSpec, Identity identity) throws Exception
    {
        if (!(connectionSpec instanceof MongoDBConnectionSpecification && authenticationSpec instanceof UserPasswordAuthenticationSpecification))
        {
            throw new IllegalStateException("Invalid ConnectionSpecification/AuthenticationSpecification. Please reach out to dev team");
        }

        MongoDBConnectionSpecification mongoDBConnectionSpec = (MongoDBConnectionSpecification) connectionSpec;
        //UserPasswordAuthenticationSpecification userPasswordAuthSpec = (UserPasswordAuthenticationSpecification) authenticationSpec;
        Credential credential = makeCredential(authenticationSpec, identity);
        Supplier<MongoClient> mongoClientSupplier;
        if (credential instanceof PlaintextUserPasswordCredential)
        {
            PlaintextUserPasswordCredential plaintextCredential = (PlaintextUserPasswordCredential) credential;
            MongoCredential mongoCredential = MongoCredential.createCredential(plaintextCredential.getUser(), ADMIN_DB, plaintextCredential.getPassword().toCharArray());
            List<ServerAddress> serverAddresses = mongoDBConnectionSpec.getServerAddresses();

            MongoClientSettings clientSettings = MongoClientSettings.builder()
                    .applyToClusterSettings(builder -> builder.hosts(serverAddresses))
                    .credential(mongoCredential)
                    .applicationName("Legend Execution Server").build();
            mongoClientSupplier = () -> MongoClients.create(clientSettings);
            return mongoClientSupplier;
        }
        else if (credential instanceof LegendKerberosCredential)
        {
            LegendKerberosCredential kerberosCredential = (LegendKerberosCredential) credential;
            MongoCredential mongoCredential = MongoCredential.createGSSAPICredential(identity.getName());
            List<ServerAddress> serverAddresses = mongoDBConnectionSpec.getServerAddresses();

            MongoClientSettings clientSettings = MongoClientSettings.builder()
                    .applyToClusterSettings(builder -> builder.hosts(serverAddresses))
                    .credential(mongoCredential)
                    .applicationName("Legend Execution Server").build();
            mongoClientSupplier = () ->
                    KerberosUtils.doAs(identity, (PrivilegedAction<MongoClient>) () -> MongoClients.create(clientSettings));
            return mongoClientSupplier;
        }

        String message = String.format("Failed to create MongoClient. Expected credential of type %s but found credential of type %s", PlaintextUserPasswordCredential.class, credential.getClass());
        throw new UnsupportedOperationException(message);

    }


}
