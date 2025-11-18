package com.kinegram.android.emrtdconnector;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Holds the results returned by the Document Validation Server.
 * <p>
 * It directly represents the `emrtd_passport` JSON Object returned by the Document Validation
 * Server. Refer to the DocVal server documentation for details.
 */
public class EmrtdPassport implements Parcelable {
    public final SODInfo sodInfo;
    public final MRZInfo mrzInfo;
    public final byte[] facePhoto;
    public final List<byte[]> signaturePhotos;
    public final AdditionalPersonalDetails additionalPersonalDetails;
    public final AdditionalDocumentDetails additionalDocumentDetails;
    public final boolean passiveAuthentication;
    public final PassiveAuthenticationDetails passiveAuthenticationDetails;
    public final CheckResult activeAuthenticationResult;
    public final CheckResult chipAuthenticationResult;
    public final String[] errors;

    /**
     * The files (SOD and DataGroups) in raw binary format.
     * This field is optional. It will only be set if the Document Validation Service
     * is configured to include this field in the response.
     */
    public final Map<String, byte[]> filesBinary;

    private final JSONObject jsonObject;

    public EmrtdPassport(JSONObject obj) throws JSONException {
        this.jsonObject = obj;
        sodInfo = SODInfo.opt(obj);
        mrzInfo = MRZInfo.opt(obj);
        facePhoto = JSONUtils.decodeB64(obj.optString("facePhoto", null));
        signaturePhotos = JSONUtils.optByteArrayList(obj, "signaturePhotos");
        additionalPersonalDetails = AdditionalPersonalDetails.opt(obj);
        additionalDocumentDetails = AdditionalDocumentDetails.opt(obj);
        passiveAuthentication = obj.getBoolean("passiveAuthentication");
        passiveAuthenticationDetails = PassiveAuthenticationDetails.opt(obj);
        activeAuthenticationResult = CheckResult.valueOf(
            obj.getString("activeAuthenticationResult"));
        chipAuthenticationResult = CheckResult.valueOf(
            obj.getString("chipAuthenticationResult"));
        errors = JSONUtils.optStringArray(obj, "errors");

        JSONObject filesBinaryObj = obj.optJSONObject("filesBinary");
        if (filesBinaryObj != null) {
            filesBinary = new HashMap<>();
            for (Iterator<String> it = filesBinaryObj.keys(); it.hasNext(); ) {
                String key = it.next();
                String b64Value = filesBinaryObj.getString(key);
                filesBinary.put(key, JSONUtils.decodeB64(b64Value));
            }
        } else {
            filesBinary = null;
        }
    }

    /**
     * @return 0
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param parcel The Parcel in which the object should be written.
     * @param i      Additional flags about how the object should be written.
     *               May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(jsonObject.toString());
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        String description = "EmrtdPassport{" +
            "\nsodInfo=" + sodInfo +
            ",\nmrzInfo=" + mrzInfo +
            ",\nfacePhoto=" + shorten(facePhoto, 32) +
            ",\nsignaturePhotos=" + shorten(signaturePhotos, 32) +
            ",\nadditionalPersonalDetails=" + additionalPersonalDetails +
            ",\nadditionalDocumentDetails=" + additionalDocumentDetails +
            ",\npassiveAuthentication=" + passiveAuthentication +
            ",\npassiveAuthenticationDetails=" + passiveAuthenticationDetails +
            ",\nactiveAuthenticationResult=" + activeAuthenticationResult +
            ",\nchipAuthenticationResult=" + chipAuthenticationResult +
            ",\nerrors=" + TextUtils.join(", ", errors);
        if (filesBinary != null) {
            description += ",\nfilesBinary=" + filesBinary;
        }
        description += "\n}";
        return description;
    }

    public JSONObject toJSON() throws JSONException {
        return new JSONObject(jsonObject.toString());
    }

    /**
     * Public CREATOR that generates instances of your {@link EmrtdPassport} class from a Parcel.
     */
    public static final Parcelable.Creator<EmrtdPassport> CREATOR
        = new Parcelable.Creator<EmrtdPassport>() {
        public EmrtdPassport createFromParcel(Parcel in) {
            try {
                return new EmrtdPassport(new JSONObject(in.readString()));
            } catch (JSONException e) {
                return null;
            }
        }

        public EmrtdPassport[] newArray(int size) {
            return new EmrtdPassport[size];
        }
    };

    public static class SODInfo {
        public final String hashAlgorithm;
        public final Map<Integer, String> hashForDataGroup = new HashMap<>();

        private SODInfo(JSONObject obj) throws JSONException {
            this.hashAlgorithm = obj.getString("hashAlgorithm");
            JSONObject objHashForDG = obj.getJSONObject("hashForDataGroup");
            Iterator<String> keys = objHashForDG.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = objHashForDG.getString(key);
                this.hashForDataGroup.put(Integer.valueOf(key), value);
            }
        }

        private static SODInfo opt(JSONObject obj) throws JSONException {
            JSONObject objSodInfo = obj.optJSONObject("sodInfo");
            return objSodInfo != null ? new SODInfo(objSodInfo) : null;
        }

        @Override
        public String toString() {
            return "SODInfo{" +
                "\n\thashAlgorithm='" + hashAlgorithm + '\'' +
                ",\n\thashForDataGroup=" + hashForDataGroup +
                "\n}";
        }
    }

    public static class MRZInfo {
        public final String documentType;
        public final String documentCode;
        public final String issuingState;
        public final String primaryIdentifier;
        public final String[] secondaryIdentifier;
        public final String nationality;
        public final String documentNumber;
        public final String dateOfBirth;
        public final String dateOfExpiry;
        public final String gender;
        public final String optionalData1;
        public final String optionalData2;

        private MRZInfo(JSONObject obj) throws JSONException {
            documentType = obj.getString("documentType");
            documentCode = obj.getString("documentCode");
            issuingState = obj.getString("issuingState");
            primaryIdentifier = obj.getString("primaryIdentifier");
            secondaryIdentifier = JSONUtils.optStringArray(obj, "secondaryIdentifier");
            nationality = obj.getString("nationality");
            documentNumber = obj.getString("documentNumber");
            dateOfBirth = obj.getString("dateOfBirth");
            dateOfExpiry = obj.getString("dateOfExpiry");
            gender = obj.getString("gender");
            optionalData1 = obj.getString("optionalData1");
            optionalData2 = obj.optString("optionalData2", null);
        }

        private static MRZInfo opt(JSONObject obj) throws JSONException {
            JSONObject objMrzInfo = obj.optJSONObject("mrzInfo");
            return objMrzInfo != null ? new MRZInfo(objMrzInfo) : null;
        }

        @Override
        public String toString() {
            return "MRZInfo{" +
                "\n\tdocumentType='" + documentType + '\'' +
                ",\n\tdocumentCode='" + documentCode + '\'' +
                ",\n\tissuingState='" + issuingState + '\'' +
                ",\n\tprimaryIdentifier='" + primaryIdentifier + '\'' +
                ",\n\tsecondaryIdentifier=" + Arrays.toString(secondaryIdentifier) +
                ",\n\tnationality='" + nationality + '\'' +
                ",\n\tdocumentNumber='" + documentNumber + '\'' +
                ",\n\tdateOfBirth='" + dateOfBirth + '\'' +
                ",\n\tdateOfExpiry='" + dateOfExpiry + '\'' +
                ",\n\tgender='" + gender + '\'' +
                ",\n\toptionalData1='" + optionalData1 + '\'' +
                ",\n\toptionalData2='" + optionalData2 + '\'' +
                "\n}";
        }
    }

    public static class AdditionalPersonalDetails {
        public final String fullNameOfHolder;
        public final String[] otherNames;
        public final String personalNumber;
        public final String fullDateOfBirth;
        public final String placeOfBirth;
        public final String[] permanentAddress;
        public final String telephone;
        public final String profession;
        public final String title;
        public final String personalSummary;
        public final byte[] proofOfCitizenshipImage;
        public final String[] otherValidTravelDocumentNumbers;
        public final String custodyInformation;

        private AdditionalPersonalDetails(JSONObject obj) throws JSONException {
            fullNameOfHolder = obj.optString("fullNameOfHolder");
            otherNames = JSONUtils.optStringArray(obj, "otherNames");
            personalNumber = obj.optString("personalNumber");
            fullDateOfBirth = obj.optString("fullDateOfBirth");
            placeOfBirth = obj.optString("placeOfBirth");
            permanentAddress = JSONUtils.optStringArray(obj, "permanentAddress");
            telephone = obj.optString("telephone");
            profession = obj.optString("profession");
            title = obj.optString("title");
            personalSummary = obj.optString("personalSummary");
            proofOfCitizenshipImage = JSONUtils.decodeB64(
                obj.optString("proofOfCitizenshipImage"));
            otherValidTravelDocumentNumbers = JSONUtils.optStringArray(
                obj, "otherValidTravelDocumentNumbers");
            custodyInformation = obj.optString("custodyInformation");
        }

        private static AdditionalPersonalDetails opt(JSONObject obj) throws JSONException {
            JSONObject objPersDet = obj.optJSONObject("additionalPersonalDetails");
            return objPersDet != null ? new AdditionalPersonalDetails(objPersDet) : null;
        }

        @Override
        public String toString() {
            return "AdditionalPersonalDetails{" +
                "\n\tfullNameOfHolder='" + fullNameOfHolder + '\'' +
                ",\n\totherNames=" + Arrays.toString(otherNames) +
                ",\n\tpersonalNumber='" + personalNumber + '\'' +
                ",\n\tfullDateOfBirth='" + fullDateOfBirth + '\'' +
                ",\n\tplaceOfBirth='" + placeOfBirth + '\'' +
                ",\n\tpermanentAddress=" + Arrays.toString(permanentAddress) +
                ",\n\ttelephone='" + telephone + '\'' +
                ",\n\tprofession='" + profession + '\'' +
                ",\n\ttitle='" + title + '\'' +
                ",\n\tpersonalSummary='" + personalSummary + '\'' +
                ",\n\tproofOfCitizenshipImage=" + Arrays.toString(proofOfCitizenshipImage) +
                ",\n\totherValidTravelDocumentNumbers=" + Arrays.toString(otherValidTravelDocumentNumbers) +
                ",\n\tcustodyInformation='" + custodyInformation + '\'' +
                "\n}";
        }
    }

    public static class AdditionalDocumentDetails {
        public final String issuingAuthority;
        public final String dateOfIssue;
        public final String namesOfOtherPersons;
        public final String endorsementsAndObservations;
        public final String taxOrExitRequirements;
        public final byte[] imageOfFront;
        public final byte[] imageOfRear;
        public final String dateAndTimeOfPersonalization;
        public final String personalizationSystemSerialNumber;

        private AdditionalDocumentDetails(JSONObject obj) throws JSONException {
            issuingAuthority = obj.optString("issuingAuthority");
            dateOfIssue = obj.optString("dateOfIssue");
            namesOfOtherPersons = obj.optString("namesOfOtherPersons");
            endorsementsAndObservations = obj.optString("endorsementsAndObservations");
            taxOrExitRequirements = obj.optString("taxOrExitRequirements");
            imageOfFront = JSONUtils.decodeB64(obj.optString("imageOfFront"));
            imageOfRear = JSONUtils.decodeB64(obj.optString("imageOfRear"));
            dateAndTimeOfPersonalization = obj.optString("dateAndTimeOfPersonalization");
            personalizationSystemSerialNumber =
                obj.optString("personalizationSystemSerialNumber");
        }

        private static AdditionalDocumentDetails opt(JSONObject obj) throws JSONException {
            JSONObject objDocDet = obj.optJSONObject("additionalDocumentDetails");
            return objDocDet != null ? new AdditionalDocumentDetails(objDocDet) : null;
        }

        @Override
        public String toString() {
            return "AdditionalDocumentDetails{" +
                "\n\tissuingAuthority='" + issuingAuthority + '\'' +
                ",\n\tdateOfIssue='" + dateOfIssue + '\'' +
                ",\n\tnamesOfOtherPersons='" + namesOfOtherPersons + '\'' +
                ",\n\tendorsementsAndObservations='" + endorsementsAndObservations + '\'' +
                ",\n\ttaxOrExitRequirements='" + taxOrExitRequirements + '\'' +
                ",\n\timageOfFront=" + Arrays.toString(imageOfFront) +
                ",\n\timageOfRear=" + Arrays.toString(imageOfRear) +
                ",\n\tdateAndTimeOfPersonalization='" + dateAndTimeOfPersonalization + '\'' +
                ",\n\tpersonalizationSystemSerialNumber='" + personalizationSystemSerialNumber + '\'' +
                "\n}";
        }
    }

    public static class PassiveAuthenticationDetails {
        public final Boolean sodSignatureValid;
        public final Boolean documentCertificateValid;
        public final int[] dataGroupsChecked;
        public final int[] dataGroupsWithValidHash;
        public final Boolean allHashesValid;
        public final String error;

        private PassiveAuthenticationDetails(JSONObject obj) throws JSONException {
            sodSignatureValid = JSONUtils.optBool(obj, "sodSignatureValid");
            documentCertificateValid = JSONUtils.optBool(obj, "documentCertificateValid");
            dataGroupsChecked = JSONUtils.optIntArray(obj, "dataGroupsChecked");
            dataGroupsWithValidHash = JSONUtils.optIntArray(
                obj, "dataGroupsWithValidHash");
            allHashesValid = JSONUtils.optBool(obj, "allHashesValid");
            error = obj.getString("error");
        }

        private static PassiveAuthenticationDetails opt(JSONObject obj) throws JSONException {
            JSONObject objPassAuthDet = obj.optJSONObject("passiveAuthenticationDetails");
            return objPassAuthDet != null ? new PassiveAuthenticationDetails(objPassAuthDet) : null;
        }

        @Override
        public String toString() {
            return "PassiveAuthenticationDetails{" +
                "\n\tsodSignatureValid=" + sodSignatureValid +
                ",\n\tdocumentCertificateValid=" + documentCertificateValid +
                ",\n\tdataGroupsChecked=" + Arrays.toString(dataGroupsChecked) +
                ",\n\tdataGroupsWithValidHash=" + Arrays.toString(dataGroupsWithValidHash) +
                ",\n\tallHashesValid=" + allHashesValid +
                ",\n\terror='" + error + '\'' +
                "\n}";
        }
    }

    public enum CheckResult {
        SUCCESS, FAILED, UNAVAILABLE;

        @Override
        public String toString() {
            return this.name();
        }
    }

    private static String shorten(Object any, int maxLength) {
        if (any == null) {
            return null;
        }
        String result = "" + any;
        if (result.length() > maxLength) {
            result = result.substring(0, maxLength - 3) + "...";
        }
        return result;
    }
}
