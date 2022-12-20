
# api-gatekeeper-xml-services-frontend

This service provides pages to search and perform crud operations on XML Organisations.
It also provides pages to bulk upload XML Organisations and collaborators on those XML Organisations.

## csvupload/organisations-page

An example csv for this is:
VENDORID,NAME
1,Organisation 1
2,Organisation 2
3,Organisation 3

## csvupload/users-page

An example csv for this is:
EMAIL, FIRSTNAME, LASTNAME, SERVICES, VENDORIDS, DUPLICATENAMES
hello@example.com,firstname,lastname,service1|service2,1|2,