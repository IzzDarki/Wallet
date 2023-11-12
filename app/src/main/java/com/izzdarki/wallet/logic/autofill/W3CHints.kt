package com.izzdarki.wallet.logic.autofill

// According to https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#autofilling-form-controls%3A-the-autocomplete-attribute
// (Link found on https://developer.android.com/guide/topics/text/autofill-services)
// the following hints are more likely some views (probably on web pages)
const val W3C_USERNAME_HINT = "username"
const val W3C_CURRENT_PASSWORD_HINT = "current-password"
const val W3C_EMAIL_HINT = "email"
const val W3C_TELEPHONE_HINT = "tel"
//const val W3C_TELEPHONE_COUNTRY_CODE_HINT = "tel-country-code"
//const val W3C_TELEPHONE_NATIONAL_HINT = "tel-national"
//const val W3C_TELEPHONE_AREA_CODE_HINT = "tel-area-code"
//const val W3C_TELEPHONE_LOCAL_HINT = "tel-local"
//const val W3C_TELEPHONE_LOCAL_PREFIX_HINT = "tel-local-prefix"
//const val W3C_TELEPHONE_LOCAL_SUFFIX_HINT = "tel-local-suffix"
//const val W3C_TELEPHONE_EXTENSION_HINT = "tel-extension"
//const val W3C_CREDIT_CARD_NAME = "cc-name"
//const val W3C_CREDIT_GIVEN_NAME = "cc-given-name"
//const val W3C_CREDIT_ADDITIONAL_NAME = "cc-additional-name"
//const val W3C_CREDIT_FAMILY_NAME = "cc-family-name"
const val W3C_CREDIT_CARD_NUMBER = "cc-number"
//const val W3C_CREDIT_CARD_EXP = "cc-exp"
//const val W3C_CREDIT_CARD_EXP_MONTH = "cc-exp-month"
//const val W3C_CREDIT_CARD_EXP_YEAR = "cc-exp-year"
const val W3C_CREDIT_CARD_CSC = "cc-csc"
//const val W3C_CREDIT_CARD_TYPE = "cc-type"
//const val W3C_STREET_ADDRESS = "street-address"
//const val W3C_ADDRESS_LINE1 = "address-line1"
//const val W3C_ADDRESS_LINE2 = "address-line2"
//const val W3C_ADDRESS_LINE3 = "address-line3"
//const val W3C_ADDRESS_LEVEL4 = "address-level4"
//const val W3C_ADDRESS_LEVEL3 = "address-level3"
//const val W3C_ADDRESS_LEVEL2 = "address-level2"
//const val W3C_ADDRESS_LEVEL1 = "address-level1"
//const val W3C_COUNTRY = "country"
//const val W3C_COUNTRY_NAME = "country-name"
//const val W3C_POSTAL_CODE = "postal-code"