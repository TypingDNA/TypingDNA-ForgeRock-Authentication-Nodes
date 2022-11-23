# TypingDNA Authentication Nodes <br> <sup>(v1.8.2)</sup>
 
With [TypingDNA](https://www.typingdna.com/), you can recognize people by the way they type on their keyboards (both desktop and mobile) in order to seamlessly authenticate them in your applications.

The TypingDNA Authentication Nodes provide two-factor authentication (2FA) to the ForgeRock Access Management solution, powered by our proprietary AI-based typing biometrics (i.e., “keystroke dynamics”) technology. They can be set up to create alternative authentication workflows through which the user’s typing behavior (captured as a typing pattern) is recorded and verified. You can find out more about the solutions we provide at [TypingDNA](https://www.typingdna.com/) by checking out our [API documentation](https://api.typingdna.com/index.html).

Typing patterns may be recorded either on the login form, essentially evaluating how the user types their username and password, or by including an additional login step in which the user is required to type a common short sentence.
 
In the following paragraphs, you will find out:
 

- How to make the TypingDNA Authentication Nodes available in your ForgeRock instance; 
- What nodes are provided; 
- How they may be configured; and 
- What tree configurations we recommend.

# More about TypingDNA and typing biometrics

At the heart of the typing biometrics solution offered by [TypingDNA](https://www.typingdna.com/) stands our RESTful Authentication API. Typing patterns are saved and verified through a number of methods described in detail in our [documentation](https://api.typingdna.com/index.html#api-API_Services).

For each user, several typing patterns may be collected, matched and/or stored by the RESTful Authentication API, depending on the context (e.g., mobile vs. mobile or desktop vs. desktop).
The typing behavior is collected and transformed into typing patterns by the TypingDNA JavaScript recorder class that you can host on your platform.

Our solutions are appropriate for several use cases such as [strong customer authentication (SCA)](https://www.typingdna.com/use-cases/sca-strong-customer-authentication), [3-D Secure](https://www.typingdna.com/use-cases/3d-secure-psd2.html), [multi-factor authentication (MFA)](https://www.typingdna.com/use-cases/multifactor-auth.html) and risk-based authentication. To get an idea of how typing biometrics work, be sure to check our quick demos on the [TypingDNA website](https://www.typingdna.com/demo-api.html).

To integrate our solution in the ForgeRock Identity Platform, please read on.

# Installation and Setup

Check out this step-by-step tutorial on how to integrate typing biometrics authentication API in ForgeRock’s Identity Platform through intuitive mouse-click configurations. A video tutorial is available for versions <= 1.3.0 of the TypingDNA Authentication Nodes. 

[![](https://www.typingdna.com/assets/images/forgerock/built-with-thumb-forgerock.png)](https://www.youtube.com/watch?v=eGII360admo "")

## Prerequisites
1. You have installed ForgeRock Access Management 7.x on a working instance, as described in the [AM 7.x Installation Guide](https://backstage.forgerock.com/docs/am/7/install-guide/index.html#preface).
2. You have obtained your API key and API secret by signing up for a [TypingDNA account](https://www.typingdna.com/clients/signup).
3. You have installed the TypingDNA Authentication Nodes plugin.

While the first point will not be described in detail, as it exceeds the scope of this documentation, the second and third points are addressed in a few short paragraphs below.


## Creating a TypingDNA account

[Sign up](https://www.typingdna.com/clients/signup) to create a free TypingDNA account. Should your needs exceed the limits of the free account, you will be able to upgrade to a paid subscription.

For enterprise accounts (aimed at mission-critical applications), or for any further clarifications, please [contact us](https://www.typingdna.com/contact.html) and we’ll be glad to help.


## Installing the nodes

The latest release of the TypingDNA Authentication Nodes are available [here](https://github.com/TypingDNA/TypingDNA-ForgeRock-Authentication-Nodes/tree/master/releases). Copy the jar file available at this location into the `/path/to/tomcat/webapps/openam/WEB-INF/lib/` directory from the instance where AM is deployed. Reload openam so that it recognizes the newly installed nodes.

Now the nodes are ready and you may test the 2FA authentication workflows that our plugin provides.

## Before you begin

Our plugin provides four nodes that may be included in an authentication tree to create different authentication workflow alternatives. Scroll down in the component list to find them:

<img src="https://www.typingdna.com/assets/images/forgerock/typingdna-nodes-forgerock-150.png" alt="The TypingDNA nodes in ForgeRock" width="305"/>


Each of these nodes have their purpose and settings described in the following paragraphs.

#### The TypingDNA Recorder
This node is necessary for all workflows as it collects typing behaviors and transforms them into typing patterns.

TypingDNA records typing patterns through the open source [TypingDNA class](https://api.typingdna.com/index.html#api-capture-class), available under Apache License (Version 2.0). Its [source code](https://github.com/TypingDNA/TypingDnaRecorder-JavaScript) is available on TypingDNA’s [GitHub repository](https://github.com/TypingDNA/TypingDnaRecorder-JavaScript).
In order to be able to use the recorder class, a script has to be created first. You can create one by navigating to **Realm** > **Scripts** > **+New script** and creating a Client-side Authentication script named *TypingDNA Class*.

<img src="https://www.typingdna.com/assets/images/forgerock/newscript-step1-forgerock.png" alt="Create a script in ForgeRock - step 1 - script name and type." width="460"/>

<img src="https://www.typingdna.com/assets/images/forgerock/newscript-step2-forgerock.png" alt="Create a script in ForgeRock - step 2 - script content and properties." width="490"/>


Copy the contents of the TypingDNA Class javascript file from the above-mentioned [GitHub repository](https://github.com/TypingDNA/TypingDnaRecorder-JavaScript) into the script, validate, and save it.

<img src="https://www.typingdna.com/assets/images/forgerock/typingdna-recorder-node-forgerock.png-170.png" alt="The TypingDNA Recorder Node in ForgeRock." width="280"/>


The script may now be selected in the TypingDNA Recorder.
By default, the recorder displays different messages depending on the actions being performed by the recorder and other configurations of the TypingDNA Decision Node (e.g. *Not enough patterns to perform matching. We need to enroll 2 more*, *The entered text had too many typos. Please try again*). Should you decide that they are not needed, they may be switched off.

The recorder will also display the TypingDNA visualizer on the input fields where typing patterns are collected. The visualizer may also be switched off, if desired. The TypingDNA visualizer script is automatically injected by the Recorder node.

<img src="https://www.typingdna.com/assets/images/forgerock/typingdna-recorder-visualizer-forgerock.png" alt="Sample of the TypingDNA Recorder Visualizer." width="380"/>

The recorder can also be configured to accept copying and pasting. By default, copying and pasting is disabled.

The **id** of the fields from which the typing patterns are collected must be specified in the **Recorder target IDs** attribute, and
the **id** of the submit button must be specified, too. This is needed in order to record and compute the typing pattern and other
parameters when the user clicks the *Log In* button. The default values will work only for the default authentication flows described
at the end of this document.
Make sure you use the correct field ids for your implementation.

<img src="https://www.typingdna.com/assets/images/forgerock/typingdna-recorder-node-set-ids.gif" alt="How to set recorder target ids and submit button id" width="380"/>

#### The TypingDNA Short Phrase Collector

<img src="https://www.typingdna.com/assets/images/forgerock/typingdna-short-phrase-collector-node-forgerock.png" alt="The TypingDNA Short Phrase Collector Node in ForgeRock." width="300"/>

This node provides an alternative authentication method based on typing biometrics. All users are requested to enter a short phrase so that their typing patterns are collected and verified.

To configure the node, enter the short phrase that the users will have to type. For improved accuracy, we recommend having users type more than 30 characters.

#### The TypingDNA Decision Node

<img src="https://www.typingdna.com/assets/images/forgerock/typingdna-decision-node-forgerock-181.png" alt="The TypingDNA Decision Node in ForgeRock." width="300"/>

The TypingDNA Decision Node does the heavy lifting: it handles the actual authentication logic by communicating with the TypingDNA [Authentication API](https://api.typingdna.com/index.html#api-API_Services). To allow it to do this, you’ll need to provide the API key and API secret that appear on your TypingDNA user account dashboard.

You may fine tune the way this node works by configuring the other available parameters:

- **API URL** - the API URL (e.g. https://api.typingdna.com)
- **API key** - the API key from your TypingDNA account
- **API secret** - the API secret from your TypingDNA account
- **Retries** - how many times a user is allowed to retry an authentication if it fails (i.e., user not recognized or an error appears) (default: 0)
- **Authentication API Configuration** - selecting Basic will use all Authentication API default settings for auto-enroll, minimum number of enrollments, and thresholds for auto-enroll and verification. Also, all requests will use the [/auto endpoint](https://api.typingdna.com/#api-API_Services-Standard-auto) that is free for all types of Authentication API clients. Advanced will use the [/verify endpoint](https://api.typingdna.com/#api-API_Services-Advanced-verifyTypingPattern), and it's behavior can be configured via the **API Settings** menu in the **TypingDNA Dashboard** for **Authentication API**. Using advanced configuration with a Starter (free) account will result in fail.
- **Hash algorithm** - the hashing algorithm used to anonymize the user IDs before sending them to the TypingDNA Authentication API. 
- **Salt** - a string that will be used when anonymizing the user ID to provide extra security (e.g., user name or user email) (empty by default).
- **Request identifier** - an optional parameter that may be used to identify requests coming from the specific ForgeRock authentication tree. The identifier will also appear in your TypingDNA logs (default: ForgeRock).
- **Request time out** - Time in milliseconds (1s = 1000ms) after which each request to the TypingDNA Authentication API should timeout if no response was received (default: 8000).
- **Authenticate after enrollments** (*Removed. Starting from versions >1.3.0 is always on*) - specifies whether the user would need to pass the authentication right after the enrollment itself is completed (default: off)
- __Number of enrollments\*__ (*removed in versions >1.3.0*) - how many enrollments are required before the user can be authenticated. We recommend starting with at least 2 enrollments (default: 3).
- __Match threshold\*__ (*removed in versions >1.3.0*) - the minimum net score that a user needs to reach to be successfully authenticated. We recommend starting at 70 (Read more about the net score in our [documentation](https://api.typingdna.com/index.html#api-API_Services-verifyTypingPattern)) (default: 70).
- __Auto-enroll threshold\*__ (*removed in versions >1.3.0*) - the minimum score a user has to reach for the typing pattern to be added to the profile. The matching threshold requirements also have to be met. (Read more about the score in our [documentation](https://api.typingdna.com/index.html#api-API_Services-verifyTypingPattern)) (default: 90).
 
__\*__ All these settings can be configured now via the **API Settings** menu in the **TypingDNA Dashboard** for **Authentication API**, if *advanced* Authentication API configuration is selected. The Authentication API defaults are used otherwise.  
 
The outcomes of this node are to be interpreted as follows:
- **Enroll** - This outcome is achieved if the user’s number of saved patterns was lower than the **Number of Enrollments**. The newly presented typing pattern will be saved to the profile. In this case, no authentication is actually performed. For passive enrollment, you will need to continue the flow to an alternative authentication node or to success; for active enrollment you will need to link this outcome back to the page node where the typing patterns are collected (i.e. the login page or the short phrase page).
- **Initial enrollment complete** - This outcome is achieved when the user's number of saved patterns is equal to that needed for enrollment. The minimum number of patterns for initial enrollment can be configured from the __API Settings__ menu in the __TypingDNA Dashboard__ for __Authentication API__. This menu is available only for paid __Authentication API__ plans. 
- **Retry** - This outcome is achieved if the authentication fails and the number of Retries is higher than or equal to the current attempt number. The authentication can fail either because the **Match threshold** has not been reached, or because of a non-critical error (which could be overcome by trying again). This outcome should be linked back to the page node where the typing patterns are collected.
- **Fail** - This outcome is achieved when a critical error occurs (e.g. invalid API credentials). This would be linked to an alternative authentication node or even Failure.
- **Match** - This outcome is achieved when the authentication is successful. For this, the net score of the authentication must exceed the **Match threshold**. This outcome would be linked to Success.
- **No Match** - This outcome is activated if the authentication fails and the number of Retries is lower than the current attempt number. The authentication can fail either because the **Match threshold** has not been reached, or because of a non-critical error. This outcome would be linked to an alternative authentication node.

IMPORTANT: The Decision Node expects the username to be in the shared state. The username is added automatically in the shared state by the **Username Collector** node, or an **Identify Existing User** node. Any node that adds the username in the shared state can be used.

#### The TypingDNA Reset Profile Node

<img src="https://www.typingdna.com/assets/images/forgerock/typingdna-reset-profile-node-forgerock-181.png" alt="The TypingDNA Reset Profile Node in ForgeRock." width="300"/>

The TypingDNA Reset Profile Node, as the name suggests, is used for resetting a user profile. This means that all the patterns recorded for this user will be deleted, and the user will be re-enrolled the next time it logs in. You'll need to use the same API key and API secret as in the TypingDNA Decision Node, as well as the same **salt**.

You can configure the node through these parameters:

- **API URL** - the API URL (e.g. https://api.typingdna.com)
- **API key** - the API key from your TypingDNA account
- **API secret** - the API secret from your TypingDNA account
- **Hash algorithm** - the  hashing algorithm used to anonymize the usernames before sending them to the TypingDNA Authentication API.
- **Salt** - a string that will be used when anonymizing the user ID to provide extra security (e.g., user name or user email) (empty by default).
- **Request identifier** - an optional parameter that may be used to identify requests coming from the specific ForgeRock authentication tree. The identifier will also appear in your TypingDNA logs (default: ForgeRock).
- **Request time out** - Time in milliseconds (1s = 1000ms) after which each request to the TypingDNA Authentication API should timeout if no response was received (default: 8000).

The outcomes of this node are to be interpreted as follows:

- **Success** - This outcome is achieved when the profile was successfully deleted.
- **Error** - This outcome is achieved when a critical error occurs (e.g. invalid API credentials). This would be linked to an alternative node, a Retry Limit Decision node or even Failure.

IMPORTANT: The Reset Profile Node expects the username to be in the shared state. The username is added automatically in the shared state by the Username Collector node, or an Identify Existing User node. Any node that adds the username in the shared state can be used.

## Creating the authentication tree with the TypingDNA Authentication Nodes

### Authentication trees

The TypingDNA Authentication Nodes can be included in a custom authentication workflow that best serves your needs. However, to illustrate the ways in which they may be integrated, we’ll present the two most common scenarios:


- Login-based typing biometrics authentication, and
- Short phrase-based typing biometrics authentication.

Regardless of the workflow that you’ll employ, the enrollment of typing patterns may be passive or active. Active enrollment prompts the user to provide the enrollment typing patterns upfront (e.g., if the Number of Enrollments parameter is set to 3, the user will be prompted to type the string three times in a row before continuing the authentication flow). Passive enrollment allows the user to continue the authentication flow each time an enrollment happens, and, in most scenarios, be authenticated using another authentication method until the profile is fully enrolled.

#### Login based typing biometrics authentication
The authentication tree below uses two of the TypingDNA nodes that are available in our plugin. 

<img src="https://www.typingdna.com/assets/images/forgerock/username-password-authentication-tree-typingdna-nodes-forgerock-180.png" alt="A sample Username and Password Authentication Tree in ForgeRock, integrated with the TypingDNA Nodes." width="900"/>

The TypingDNA Recorder is added in the Login Page Node to collect the typing behavior of the user while typing both the username and password. A typing pattern is created based on the typing behavior and passed on to the TypingDNA Decision Node (only if the Data Store Decision Node confirms that the username and password belong to an entry from the AM Identities DB). The TypingDNA Decision Node, depending on how it was configured, will either enroll the typing pattern or check whether it matches an existing profile, depending on how many enrollments are required and how many are already enrolled.

The authentication tree from the screenshot above represents an active enrollment. Should the TypingDNA Decision Node take the Enroll path, the user is taken back to the Login Page Node where a new typing pattern will be recorded and stored. This loop is continued until the user’s profile has a number of typing patterns that is equal to the Number of Enrollments parameter.

An alternative to this is passive enrollment in which case the user is automatically logged in or asked for another factor after each enrollment. The only change that is needed on the tree is  linking the Enroll decision to the Success node (for completing the authentication) or to the node of another authentication method (for a step-up authentication).

#### Short phrase-based typing biometrics authentication
The authentication tree below uses three of the nodes that are available in this plugin.

<img src="https://www.typingdna.com/assets/images/forgerock/short-phrase-authentication-tree-typingdna-nodes-forgerock-180.png" alt="A sample Short Phrase 2FA Authentication Tree in ForgeRock, integrated with the TypingDNA Nodes." width="900"/>

In this case, the login Page Node no longer includes the TypingDNA Recorder which, in this scenario, is used in conjunction with the TypingDNA Short Phrase Collector within a different Page Node. Consequently, after the user was authenticated by the Data Store Decision, the user has to type a specific short phrase that can be customized in the TypingDNA Short Phrase Collector parameters. While the user is typing, the TypingDNA Recorder collects the typing behavior of the user and sends the resulting typing pattern to be processed by the TypingDNA Decision Node.

<img src="https://www.typingdna.com/assets/images/forgerock/short-phrase-2fa-authentication-sample-typingdna-nodes-forgerock.png" alt="A sample Short Phrase Dialog in ForgeRock, integrated with the TypingDNA Nodes." width="450"/>

Similar to Login-based typing biometrics authentication, the user and/or the typing pattern might be enrolled or verified and a decision will be made depending on the specific scenario. Passive and active enrollment are again possible just like the Login-based typing biometrics authentication tree.

#### Profile reset flow
The flow belows is an example of how profile reset can be implemented:

<img src="https://www.typingdna.com/assets/images/forgerock/reset-profile-tree-typingdna-nodes-forgerock.png" alt="A example of profile reset" width="900"/>

# Licensing and terms of use

The copyright of this document as well as the source code of the TypingDNA integration for ForgeRock is vested by TypingDNA Inc. The TypingDNA integration for ForgeRock is subject to an Apache License, version 2.0 (“the License”). You may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0. 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. The use of the TypingDNA Authentication API is subject to TypingDNA’s terms and conditions. You may consult the terms and conditions at any time at https://www.typingdna.com/legal/legal.html.
