/*
 * eID TSL Project.
 * Copyright (C) 2009 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.eid.tsl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Constants;
import org.apache.xpath.XPathAPI;
import org.bouncycastle.openssl.PEMWriter;
import org.etsi.uri._01903.v1_3.CertIDListType;
import org.etsi.uri._01903.v1_3.CertIDType;
import org.etsi.uri._01903.v1_3.DigestAlgAndValueType;
import org.etsi.uri._01903.v1_3.QualifyingPropertiesType;
import org.etsi.uri._01903.v1_3.SignedPropertiesType;
import org.etsi.uri._01903.v1_3.SignedSignaturePropertiesType;
import org.etsi.uri._02231.v2_.AddressType;
import org.etsi.uri._02231.v2_.ElectronicAddressType;
import org.etsi.uri._02231.v2_.InternationalNamesType;
import org.etsi.uri._02231.v2_.MultiLangStringType;
import org.etsi.uri._02231.v2_.NextUpdateType;
import org.etsi.uri._02231.v2_.NonEmptyMultiLangURIListType;
import org.etsi.uri._02231.v2_.NonEmptyMultiLangURIType;
import org.etsi.uri._02231.v2_.NonEmptyURIListType;
import org.etsi.uri._02231.v2_.ObjectFactory;
import org.etsi.uri._02231.v2_.PolicyOrLegalnoticeType;
import org.etsi.uri._02231.v2_.PostalAddressListType;
import org.etsi.uri._02231.v2_.PostalAddressType;
import org.etsi.uri._02231.v2_.TSLSchemeInformationType;
import org.etsi.uri._02231.v2_.TSPType;
import org.etsi.uri._02231.v2_.TrustServiceProviderListType;
import org.etsi.uri._02231.v2_.TrustStatusListType;
import org.joda.time.DateTime;
import org.w3._2000._09.xmldsig_.DigestMethodType;
import org.w3._2000._09.xmldsig_.X509IssuerSerialType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Trust Service List.
 * 
 * @author fcorneli
 * 
 */
public class TrustServiceList {

	private static final Log LOG = LogFactory.getLog(TrustServiceList.class);

	public static final String TSL_TAG = "http://uri.etsi.org/02231/TSLtag";

	public static final String TSL_TYPE = "http://uri.etsi.org/TrstSvc/eSigDir-1999-93-EC-TrustedList/TSLType/generic";

	private static final String XADES_TYPE = "http://uri.etsi.org/01903#SignedProperties";

	public static final String STATUS_DETERMINATION_APPROPRIATE = "http://uri.etsi.org/TrstSvc/eSigDir-1999-93-EC-TrustedList/StatusDetn/appropriate";

	public static final String SCHEME_RULE_COMMON = "http://uri.etsi.org/TrstSvc/eSigDir-1999-93-EC-TrustedList/schemerules/common";

	private TrustStatusListType trustStatusList;

	private Document tslDocument;

	private List<TrustServiceProvider> trustServiceProviders;

	private boolean changed;

	private final List<ChangeListener> changeListeners;

	private final ObjectFactory objectFactory;

	private final DatatypeFactory datatypeFactory;

	private final org.etsi.uri._01903.v1_3.ObjectFactory xadesObjectFactory;

	private final org.w3._2000._09.xmldsig_.ObjectFactory xmldsigObjectFactory;

	protected TrustServiceList() {
		super();
		this.changed = true;
		this.changeListeners = new LinkedList<ChangeListener>();
		this.objectFactory = new ObjectFactory();
		this.xadesObjectFactory = new org.etsi.uri._01903.v1_3.ObjectFactory();
		this.xmldsigObjectFactory = new org.w3._2000._09.xmldsig_.ObjectFactory();
		try {
			this.datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException("datatype config error: "
					+ e.getMessage(), e);
		}
	}

	protected TrustServiceList(TrustStatusListType trustStatusList,
			Document tslDocument) {
		this.trustStatusList = trustStatusList;
		this.tslDocument = tslDocument;
		this.changeListeners = new LinkedList<ChangeListener>();
		this.objectFactory = new ObjectFactory();
		this.xadesObjectFactory = new org.etsi.uri._01903.v1_3.ObjectFactory();
		this.xmldsigObjectFactory = new org.w3._2000._09.xmldsig_.ObjectFactory();
		try {
			this.datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException("datatype config error: "
					+ e.getMessage(), e);
		}
	}

	public void addChangeListener(ChangeListener changeListener) {
		this.changeListeners.add(changeListener);
	}

	public void removeChangeListener(ChangeListener changeListener) {
		this.changeListeners.remove(changeListener);
	}

	private void notifyChangeListeners() {
		for (ChangeListener changeListener : changeListeners) {
			changeListener.changed();
		}
	}

	public boolean hasChanged() {
		return this.changed;
	}

	public String getSchemeName() {
		Locale locale = Locale.getDefault();
		return getSchemeName(locale);
	}

	/**
	 * Sets the scheme name according to the default locale.
	 * 
	 * @param schemeName
	 */
	public void setSchemeName(String schemeName) {
		Locale locale = Locale.getDefault();
		setSchemeName(schemeName, locale);
	}

	public void setSchemeOperatorName(String schemeOperatorName) {
		Locale locale = Locale.getDefault();
		setSchemeOperatorName(schemeOperatorName, locale);
	}

	private void clearDocumentCacheAndSetChanged() {
		/*
		 * The XML signature should be regenerated anyway, so clear the TSL DOM
		 * object.
		 */
		this.tslDocument = null;
		setChanged();
	}

	private void setChanged() {
		this.changed = true;
		notifyChangeListeners();
	}

	private TrustStatusListType getTrustStatusList() {
		if (null == this.trustStatusList) {
			this.trustStatusList = this.objectFactory
					.createTrustStatusListType();
		}
		return this.trustStatusList;
	}

	private TSLSchemeInformationType getSchemeInformation() {
		TrustStatusListType trustStatusList = getTrustStatusList();
		TSLSchemeInformationType tslSchemeInformation = trustStatusList
				.getSchemeInformation();
		if (null == tslSchemeInformation) {
			tslSchemeInformation = this.objectFactory
					.createTSLSchemeInformationType();
			trustStatusList.setSchemeInformation(tslSchemeInformation);
		}
		return tslSchemeInformation;
	}

	public void setSchemeName(String schemeName, Locale locale) {
		TSLSchemeInformationType tslSchemeInformation = getSchemeInformation();
		InternationalNamesType i18nSchemeName = tslSchemeInformation
				.getSchemeName();
		if (null == i18nSchemeName) {
			i18nSchemeName = this.objectFactory.createInternationalNamesType();
			tslSchemeInformation.setSchemeName(i18nSchemeName);
		}
		TrustServiceListUtils.setValue(schemeName, locale, i18nSchemeName);
		/*
		 * Also notify the listeners that we've changed content.
		 */
		clearDocumentCacheAndSetChanged();
	}

	public void setSchemeOperatorName(String schemeOperatorName, Locale locale) {
		TSLSchemeInformationType tslSchemeInformation = getSchemeInformation();
		InternationalNamesType i18nSchemeOperatorName = tslSchemeInformation
				.getSchemeOperatorName();
		if (null == i18nSchemeOperatorName) {
			i18nSchemeOperatorName = this.objectFactory
					.createInternationalNamesType();
			tslSchemeInformation.setSchemeOperatorName(i18nSchemeOperatorName);
		}
		TrustServiceListUtils.setValue(schemeOperatorName, locale,
				i18nSchemeOperatorName);
		clearDocumentCacheAndSetChanged();
	}

	private AddressType getSchemeOperatorAddress() {
		TSLSchemeInformationType schemeInformation = getSchemeInformation();
		AddressType schemeOperatorAddress = schemeInformation
				.getSchemeOperatorAddress();
		if (null == schemeOperatorAddress) {
			schemeOperatorAddress = this.objectFactory.createAddressType();
			schemeInformation.setSchemeOperatorAddress(schemeOperatorAddress);
		}
		return schemeOperatorAddress;
	}

	public void setSchemeOperatorPostalAddress(PostalAddressType postalAddress,
			Locale locale) {
		AddressType schemeOperatorAddress = getSchemeOperatorAddress();
		PostalAddressListType postalAddresses = schemeOperatorAddress
				.getPostalAddresses();
		if (null == postalAddresses) {
			postalAddresses = this.objectFactory.createPostalAddressListType();
			schemeOperatorAddress.setPostalAddresses(postalAddresses);
		}
		/*
		 * First try to locate an existing address for the given locale.
		 */
		PostalAddressType existingPostalAddress = null;
		for (PostalAddressType currentPostalAddress : postalAddresses
				.getPostalAddress()) {
			if (currentPostalAddress.getLang().toLowerCase().equals(
					locale.getLanguage())) {
				existingPostalAddress = currentPostalAddress;
				break;
			}
		}
		if (null != existingPostalAddress) {
			/*
			 * Update the existing postal address.
			 */
			existingPostalAddress.setStreetAddress(postalAddress
					.getStreetAddress());
			existingPostalAddress.setPostalCode(postalAddress.getPostalCode());
			existingPostalAddress.setLocality(postalAddress.getLocality());
			existingPostalAddress.setStateOrProvince(postalAddress
					.getStateOrProvince());
			existingPostalAddress
					.setCountryName(postalAddress.getCountryName());
		} else {
			LOG.debug("add postal address: " + locale.getLanguage());
			/*
			 * Add the new postal address. We really have to create a copy into
			 * a new JAXB object. This allows a caller to reuse a postal address
			 * JAXB data structure without running into trouble with the JAXB
			 * marshaller.
			 */
			PostalAddressType newPostalAddress = this.objectFactory
					.createPostalAddressType();
			newPostalAddress.setLang(locale.getLanguage());
			newPostalAddress.setStreetAddress(postalAddress.getStreetAddress());
			newPostalAddress.setPostalCode(postalAddress.getPostalCode());
			newPostalAddress.setLocality(postalAddress.getLocality());
			newPostalAddress.setStateOrProvince(postalAddress
					.getStateOrProvince());
			newPostalAddress.setCountryName(postalAddress.getCountryName());
			postalAddresses.getPostalAddress().add(newPostalAddress);
		}
	}

	public List<String> getSchemeOperatorElectronicAddresses() {
		if (null == this.trustStatusList) {
			return null;
		}
		TSLSchemeInformationType tslSchemeInformation = this.trustStatusList
				.getSchemeInformation();
		if (null == tslSchemeInformation) {
			return null;
		}
		AddressType address = tslSchemeInformation.getSchemeOperatorAddress();
		if (null == address) {
			return null;
		}
		ElectronicAddressType electronicAddress = address
				.getElectronicAddress();
		if (null == electronicAddress) {
			return null;
		}
		return electronicAddress.getURI();
	}

	public void setSchemeOperatorElectronicAddresses(List<String> addresses) {
		AddressType schemeOperatorAddress = getSchemeOperatorAddress();
		ElectronicAddressType electronicAddress = schemeOperatorAddress
				.getElectronicAddress();
		if (null == electronicAddress) {
			electronicAddress = this.objectFactory
					.createElectronicAddressType();
			schemeOperatorAddress.setElectronicAddress(electronicAddress);
		}
		List<String> electronicAddresses = electronicAddress.getURI();
		electronicAddresses.clear();
		for (String address : addresses) {
			electronicAddresses.add(address);
		}
	}

	public String getSchemeName(Locale locale) {
		if (null == this.trustStatusList) {
			return null;
		}
		TSLSchemeInformationType tslSchemeInformation = this.trustStatusList
				.getSchemeInformation();
		InternationalNamesType i18nSchemeName = tslSchemeInformation
				.getSchemeName();
		String name = TrustServiceListUtils.getValue(i18nSchemeName, locale);
		return name;
	}

	public String getSchemeOperatorName() {
		Locale locale = Locale.getDefault();
		return getSchemeOperatorName(locale);
	}

	public String getSchemeOperatorName(Locale locale) {
		if (null == this.trustStatusList) {
			return null;
		}
		TSLSchemeInformationType tslSchemeInformation = this.trustStatusList
				.getSchemeInformation();
		InternationalNamesType i18nSchemeOperatorName = tslSchemeInformation
				.getSchemeOperatorName();
		String name = TrustServiceListUtils.getValue(i18nSchemeOperatorName,
				locale);
		return name;
	}

	public void addSchemeInformationUri(String uri, Locale locale) {
		TSLSchemeInformationType schemeInformation = getSchemeInformation();
		NonEmptyMultiLangURIListType schemeInformationUriList = schemeInformation
				.getSchemeInformationURI();
		if (null == schemeInformationUriList) {
			schemeInformationUriList = this.objectFactory
					.createNonEmptyMultiLangURIListType();
			schemeInformation.setSchemeInformationURI(schemeInformationUriList);
		}
		NonEmptyMultiLangURIType i18nUri = this.objectFactory
				.createNonEmptyMultiLangURIType();
		i18nUri.setLang(locale.getLanguage());
		i18nUri.setValue(uri);
		schemeInformationUriList.getURI().add(i18nUri);
	}

	public List<String> getSchemeInformationUris() {
		if (null == this.trustStatusList) {
			return null;
		}
		TSLSchemeInformationType schemeInformation = this.trustStatusList
				.getSchemeInformation();
		if (null == schemeInformation) {
			return null;
		}
		NonEmptyMultiLangURIListType schemeInformationUriList = schemeInformation
				.getSchemeInformationURI();
		if (null == schemeInformationUriList) {
			return null;
		}
		List<NonEmptyMultiLangURIType> uris = schemeInformationUriList.getURI();
		List<String> results = new LinkedList<String>();
		for (NonEmptyMultiLangURIType uri : uris) {
			results.add(uri.getValue());
		}
		return results;
	}

	public void setStatusDeterminationApproach(
			String statusDeterminationApproach) {
		TSLSchemeInformationType schemeInformation = getSchemeInformation();
		schemeInformation
				.setStatusDeterminationApproach(statusDeterminationApproach);
	}

	public String getStatusDeterminationApproach() {
		if (null == this.trustStatusList) {
			return null;
		}
		TSLSchemeInformationType schemeInformation = this.trustStatusList
				.getSchemeInformation();
		if (null == schemeInformation) {
			return null;
		}
		String statusDeterminationApproach = schemeInformation
				.getStatusDeterminationApproach();
		return statusDeterminationApproach;
	}

	public List<TrustServiceProvider> getTrustServiceProviders() {
		if (null != this.trustServiceProviders) {
			// only load once
			return this.trustServiceProviders;
		}
		this.trustServiceProviders = new LinkedList<TrustServiceProvider>();
		if (null == this.trustStatusList) {
			return this.trustServiceProviders;
		}
		TrustServiceProviderListType trustServiceProviderList = this.trustStatusList
				.getTrustServiceProviderList();
		if (null == trustServiceProviderList) {
			return this.trustServiceProviders;
		}
		List<TSPType> tsps = trustServiceProviderList.getTrustServiceProvider();
		for (TSPType tsp : tsps) {
			TrustServiceProvider trustServiceProvider = new TrustServiceProvider(
					tsp);
			this.trustServiceProviders.add(trustServiceProvider);
		}
		return this.trustServiceProviders;
	}

	public String getType() {
		try {
			marshall();
		} catch (Exception e) {
			throw new RuntimeException("marshall error: " + e.getMessage());
		}
		TSLSchemeInformationType tslSchemeInformation = this.trustStatusList
				.getSchemeInformation();
		String type = tslSchemeInformation.getTSLType();
		return type;
	}

	public BigInteger getSequenceNumber() {
		if (null == this.trustStatusList) {
			return null;
		}
		TSLSchemeInformationType tslSchemeInformation = this.trustStatusList
				.getSchemeInformation();
		BigInteger sequenceNumber = tslSchemeInformation.getTSLSequenceNumber();
		return sequenceNumber;
	}

	public Date getIssueDate() {
		if (null == this.trustStatusList) {
			return null;
		}
		TSLSchemeInformationType tslSchemeInformation = this.trustStatusList
				.getSchemeInformation();
		XMLGregorianCalendar xmlGregorianCalendar = tslSchemeInformation
				.getListIssueDateTime();
		return xmlGregorianCalendar.toGregorianCalendar().getTime();
	}

	public boolean hasSignature() {
		if (null == this.tslDocument) {
			/*
			 * Even if the JAXB TSL still has a signature, it's probably already
			 * invalid.
			 */
			return false;
		}
		Node signatureNode = getSignatureNode();
		if (null == signatureNode) {
			return false;
		}
		return true;
	}

	public X509Certificate verifySignature() {
		if (null == this.tslDocument) {
			LOG.debug("first save the document");
			return null;
		}

		Node signatureNode = getSignatureNode();
		if (null == signatureNode) {
			LOG.debug("no ds:Signature element present");
			return null;
		}

		KeyInfoKeySelector keyInfoKeySelector = new KeyInfoKeySelector();
		DOMValidateContext valContext = new DOMValidateContext(
				keyInfoKeySelector, signatureNode);
		XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory
				.getInstance("DOM");
		XMLSignature signature;
		try {
			signature = xmlSignatureFactory.unmarshalXMLSignature(valContext);
		} catch (MarshalException e) {
			throw new RuntimeException("XML signature parse error: "
					+ e.getMessage(), e);
		}
		boolean coreValidity;
		try {
			coreValidity = signature.validate(valContext);
		} catch (XMLSignatureException e) {
			throw new RuntimeException(
					"XML signature error: " + e.getMessage(), e);
		}

		// TODO: check what has been signed

		if (coreValidity) {
			LOG.debug("signature valid");
			return keyInfoKeySelector.getCertificate();
		}
		LOG.debug("signature invalid");

		return null;
	}

	private Node getSignatureNode() {
		Element nsElement = this.tslDocument.createElement("ns");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
				XMLSignature.XMLNS);
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:tsl",
				"http://uri.etsi.org/02231/v2#");

		Node signatureNode;
		try {
			signatureNode = XPathAPI.selectSingleNode(this.tslDocument,
					"tsl:TrustServiceStatusList/ds:Signature", nsElement);
		} catch (TransformerException e) {
			throw new RuntimeException("XPath error: " + e.getMessage(), e);
		}
		return signatureNode;
	}

	private void marshall() throws JAXBException, ParserConfigurationException {
		/*
		 * Assign a unique XML Id to the TSL for signing purposes.
		 */
		String tslId = "tsl-" + UUID.randomUUID().toString();
		TrustStatusListType trustStatusList = getTrustStatusList();
		trustStatusList.setId(tslId);

		/*
		 * TSLTag
		 */
		trustStatusList.setTSLTag(TSL_TAG);

		/*
		 * Scheme Information - TSL version identifier.
		 */
		TSLSchemeInformationType schemeInformation = trustStatusList
				.getSchemeInformation();
		if (null == schemeInformation) {
			schemeInformation = this.objectFactory
					.createTSLSchemeInformationType();
			trustStatusList.setSchemeInformation(schemeInformation);
		}
		schemeInformation.setTSLVersionIdentifier(BigInteger.valueOf(3));

		/*
		 * Scheme Information - TSL sequence number
		 */
		schemeInformation.setTSLSequenceNumber(BigInteger.valueOf(1));

		/*
		 * Scheme Information - TSL Type
		 */
		schemeInformation.setTSLType(TSL_TYPE);

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document document = documentBuilder.newDocument();

		JAXBContext jaxbContext = JAXBContext
				.newInstance(
						ObjectFactory.class,
						org.etsi.uri.trstsvc.svcinfoext.esigdir_1999_93_ec_trustedlist.ObjectFactory.class);
		Marshaller marshaller = jaxbContext.createMarshaller();
		LOG.debug("marshaller type: " + marshaller.getClass().getName());
		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",
				new TSLNamespacePrefixMapper());
		ObjectFactory objectFactory = new ObjectFactory();
		JAXBElement<TrustStatusListType> trustStatusListElement = objectFactory
				.createTrustServiceStatusList(trustStatusList);
		marshaller.marshal(trustStatusListElement, document);

		this.tslDocument = document;
	}

	public void sign(PrivateKey privateKey, X509Certificate certificate)
			throws IOException {
		LOG.debug("sign with: " + certificate.getSubjectX500Principal());
		if (null == this.tslDocument) {
			/*
			 * Marshall to DOM.
			 */
			try {
				marshall();
			} catch (Exception e) {
				throw new IOException("marshaller error: " + e.getMessage(), e);
			}
		}

		/*
		 * Remove existing XML signature from DOM.
		 */
		Node signatureNode = getSignatureNode();
		if (null != signatureNode) {
			signatureNode.getParentNode().removeChild(signatureNode);
		}

		String tslId = this.trustStatusList.getId();

		/*
		 * Create new XML signature.
		 */
		try {
			xmlSign(privateKey, certificate, tslId);
		} catch (Exception e) {
			throw new IOException("XML sign error: " + e.getMessage(), e);
		}
		setChanged();
	}

	private void xmlSign(PrivateKey privateKey, X509Certificate certificate,
			String tslId) throws NoSuchAlgorithmException,
			InvalidAlgorithmParameterException, MarshalException,
			XMLSignatureException {
		XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance(
				"DOM", new org.jcp.xml.dsig.internal.dom.XMLDSigRI());
		LOG.debug("xml signature factory: "
				+ signatureFactory.getClass().getName());
		LOG.debug("loader: " + signatureFactory.getClass().getClassLoader());
		XMLSignContext signContext = new DOMSignContext(privateKey,
				this.tslDocument.getDocumentElement());
		signContext.putNamespacePrefix(XMLSignature.XMLNS, "ds");

		DigestMethod digestMethod = signatureFactory.newDigestMethod(
				DigestMethod.SHA256, null);
		List<Reference> references = new LinkedList<Reference>();
		List<Transform> transforms = new LinkedList<Transform>();
		transforms.add(signatureFactory.newTransform(Transform.ENVELOPED,
				(TransformParameterSpec) null));
		Transform exclusiveTransform = signatureFactory
				.newTransform(CanonicalizationMethod.EXCLUSIVE,
						(TransformParameterSpec) null);
		transforms.add(exclusiveTransform);

		Reference reference = signatureFactory.newReference("#" + tslId,
				digestMethod, transforms, null, null);
		references.add(reference);

		String signatureId = "xmldsig-" + UUID.randomUUID().toString();
		List<XMLObject> objects = new LinkedList<XMLObject>();
		addXadesBes(signatureFactory, this.tslDocument, signatureId,
				certificate, references, objects);

		SignatureMethod signatureMethod = signatureFactory.newSignatureMethod(
				SignatureMethod.RSA_SHA1, null);
		CanonicalizationMethod canonicalizationMethod = signatureFactory
				.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE,
						(C14NMethodParameterSpec) null);
		SignedInfo signedInfo = signatureFactory.newSignedInfo(
				canonicalizationMethod, signatureMethod, references);

		List<Object> keyInfoContent = new LinkedList<Object>();

		KeyInfoFactory keyInfoFactory = KeyInfoFactory.getInstance();
		List<Object> x509DataObjects = new LinkedList<Object>();
		x509DataObjects.add(certificate);
		x509DataObjects.add(keyInfoFactory.newX509IssuerSerial(certificate
				.getIssuerX500Principal().toString(), certificate
				.getSerialNumber()));
		X509Data x509Data = keyInfoFactory.newX509Data(x509DataObjects);
		keyInfoContent.add(x509Data);

		KeyValue keyValue;
		try {
			keyValue = keyInfoFactory.newKeyValue(certificate.getPublicKey());
		} catch (KeyException e) {
			throw new RuntimeException("key exception: " + e.getMessage(), e);
		}
		keyInfoContent.add(keyValue);

		KeyInfo keyInfo = keyInfoFactory.newKeyInfo(keyInfoContent);

		String signatureValueId = signatureId + "-signature-value";
		XMLSignature xmlSignature = signatureFactory.newXMLSignature(
				signedInfo, keyInfo, objects, signatureId, signatureValueId);
		xmlSignature.sign(signContext);
	}

	public void addXadesBes(XMLSignatureFactory signatureFactory,
			Document document, String signatureId,
			X509Certificate signingCertificate, List<Reference> references,
			List<XMLObject> objects) throws NoSuchAlgorithmException,
			InvalidAlgorithmParameterException {
		LOG.debug("preSign");

		// QualifyingProperties
		QualifyingPropertiesType qualifyingProperties = this.xadesObjectFactory
				.createQualifyingPropertiesType();
		qualifyingProperties.setTarget("#" + signatureId);

		// SignedProperties
		SignedPropertiesType signedProperties = this.xadesObjectFactory
				.createSignedPropertiesType();
		String signedPropertiesId = signatureId + "-xades";
		signedProperties.setId(signedPropertiesId);
		qualifyingProperties.setSignedProperties(signedProperties);

		// SignedSignatureProperties
		SignedSignaturePropertiesType signedSignatureProperties = this.xadesObjectFactory
				.createSignedSignaturePropertiesType();
		signedProperties
				.setSignedSignatureProperties(signedSignatureProperties);

		// SigningTime
		GregorianCalendar signingTime = new GregorianCalendar();
		signingTime.setTimeZone(TimeZone.getTimeZone("Z"));
		signedSignatureProperties.setSigningTime(this.datatypeFactory
				.newXMLGregorianCalendar(signingTime));

		// SigningCertificate
		CertIDListType signingCertificates = this.xadesObjectFactory
				.createCertIDListType();
		CertIDType signingCertificateId = this.xadesObjectFactory
				.createCertIDType();

		X509IssuerSerialType issuerSerial = this.xmldsigObjectFactory
				.createX509IssuerSerialType();
		issuerSerial.setX509IssuerName(signingCertificate
				.getIssuerX500Principal().toString());
		issuerSerial.setX509SerialNumber(signingCertificate.getSerialNumber());
		signingCertificateId.setIssuerSerial(issuerSerial);

		DigestAlgAndValueType certDigest = this.xadesObjectFactory
				.createDigestAlgAndValueType();
		DigestMethodType jaxbDigestMethod = xmldsigObjectFactory
				.createDigestMethodType();
		jaxbDigestMethod.setAlgorithm(DigestMethod.SHA1);
		certDigest.setDigestMethod(jaxbDigestMethod);
		MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
		byte[] digestValue;
		try {
			digestValue = messageDigest.digest(signingCertificate.getEncoded());
		} catch (CertificateEncodingException e) {
			throw new RuntimeException("certificate encoding error: "
					+ e.getMessage(), e);
		}
		certDigest.setDigestValue(digestValue);
		signingCertificateId.setCertDigest(certDigest);

		signingCertificates.getCert().add(signingCertificateId);
		signedSignatureProperties.setSigningCertificate(signingCertificates);

		// marshall XAdES QualifyingProperties
		Node qualifyingPropertiesNode = marshallQualifyingProperties(document,
				qualifyingProperties);

		// add XAdES ds:Object
		List<XMLStructure> xadesObjectContent = new LinkedList<XMLStructure>();
		xadesObjectContent.add(new DOMStructure(qualifyingPropertiesNode));
		XMLObject xadesObject = signatureFactory.newXMLObject(
				xadesObjectContent, null, null, null);
		objects.add(xadesObject);

		// add XAdES ds:Reference
		DigestMethod digestMethod = signatureFactory.newDigestMethod(
				DigestMethod.SHA256, null);
		List<Transform> transforms = new LinkedList<Transform>();
		Transform exclusiveTransform = signatureFactory
				.newTransform(CanonicalizationMethod.EXCLUSIVE,
						(TransformParameterSpec) null);
		transforms.add(exclusiveTransform);
		Reference reference = signatureFactory.newReference("#"
				+ signedPropertiesId, digestMethod, transforms, XADES_TYPE,
				null);
		references.add(reference);
	}

	private Node marshallQualifyingProperties(Document document,
			QualifyingPropertiesType qualifyingProperties) {
		Node marshallNode = document.createElement("marshall-node");
		try {
			JAXBContext jaxbContext = JAXBContext
					.newInstance(org.etsi.uri._01903.v1_3.ObjectFactory.class);
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",
					new TSLNamespacePrefixMapper());
			marshaller.marshal(this.xadesObjectFactory
					.createQualifyingProperties(qualifyingProperties),
					marshallNode);
		} catch (JAXBException e) {
			throw new RuntimeException("JAXB error: " + e.getMessage(), e);
		}
		Node qualifyingPropertiesNode = marshallNode.getFirstChild();
		return qualifyingPropertiesNode;
	}

	private void clearChanged() {
		this.changed = false;
	}

	public void save(File tslFile) throws IOException {
		LOG.debug("save to: " + tslFile.getAbsolutePath());
		if (null == this.tslDocument) {
			try {
				marshall();
			} catch (Exception e) {
				throw new IOException("marshall error: " + e.getMessage(), e);
			}
			/*
			 * Only remove existing XML signature from new (or changed) DOM
			 * documents.
			 */
			Node signatureNode = getSignatureNode();
			if (null != signatureNode) {
				signatureNode.getParentNode().removeChild(signatureNode);
			}
		}
		try {
			toFile(tslFile);
		} catch (Exception e) {
			throw new IOException(
					"DOM transformation error: " + e.getMessage(), e);
		}
		clearChanged();
	}

	private void toFile(File tslFile)
			throws TransformerFactoryConfigurationError,
			TransformerConfigurationException, TransformerException {
		Source source = new DOMSource(this.tslDocument);
		Result result = new StreamResult(tslFile);
		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		/*
		 * We have to omit the ?xml declaration if we want to embed the
		 * document.
		 */
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(source, result);
	}

	public PostalAddressType getSchemeOperatorPostalAddress(Locale locale) {
		if (null == this.trustStatusList) {
			return null;
		}
		TSLSchemeInformationType schemeInformation = this.trustStatusList
				.getSchemeInformation();
		if (null == schemeInformation) {
			return null;
		}
		AddressType schemeOperatorAddress = schemeInformation
				.getSchemeOperatorAddress();
		if (null == schemeOperatorAddress) {
			return null;
		}
		PostalAddressListType postalAddresses = schemeOperatorAddress
				.getPostalAddresses();
		if (null == postalAddresses) {
			return null;
		}
		for (PostalAddressType postalAddress : postalAddresses
				.getPostalAddress()) {
			if (postalAddress.getLang().toLowerCase().equals(
					locale.getLanguage())) {
				return postalAddress;
			}
		}
		return null;
	}

	public void addSchemeType(String schemeType) {
		TSLSchemeInformationType schemeInformation = getSchemeInformation();
		NonEmptyURIListType schemeTypeList = schemeInformation
				.getSchemeTypeCommunityRules();
		if (null == schemeTypeList) {
			schemeTypeList = this.objectFactory.createNonEmptyURIListType();
			schemeInformation.setSchemeTypeCommunityRules(schemeTypeList);
		}
		schemeTypeList.getURI().add(schemeType);
	}

	public List<String> getSchemeTypes() {
		if (null == this.trustStatusList) {
			return null;
		}
		TSLSchemeInformationType schemeInformation = this.trustStatusList
				.getSchemeInformation();
		if (null == schemeInformation) {
			return null;
		}
		NonEmptyURIListType schemeTypeList = schemeInformation
				.getSchemeTypeCommunityRules();
		if (null == schemeTypeList) {
			return null;
		}
		List<String> schemeTypes = schemeTypeList.getURI();
		return schemeTypes;
	}

	public void setSchemeTerritory(String schemeTerritory) {
		TSLSchemeInformationType schemeInformation = getSchemeInformation();
		schemeInformation.setSchemeTerritory(schemeTerritory);
	}

	public String getSchemeTerritory() {
		if (null == this.trustStatusList) {
			return null;
		}
		TSLSchemeInformationType schemeInformation = this.trustStatusList
				.getSchemeInformation();
		if (null == schemeInformation) {
			return null;
		}
		String schemeTerritory = schemeInformation.getSchemeTerritory();
		return schemeTerritory;
	}

	public void addLegalNotice(String legalNotice, Locale locale) {
		TSLSchemeInformationType schemeInformation = getSchemeInformation();
		PolicyOrLegalnoticeType policyOrLegalnotice = schemeInformation
				.getPolicyOrLegalNotice();
		if (null == policyOrLegalnotice) {
			policyOrLegalnotice = this.objectFactory
					.createPolicyOrLegalnoticeType();
			schemeInformation.setPolicyOrLegalNotice(policyOrLegalnotice);
		}
		List<MultiLangStringType> tslLegalNotices = policyOrLegalnotice
				.getTSLLegalNotice();

		MultiLangStringType tslLegalNotice = this.objectFactory
				.createMultiLangStringType();
		tslLegalNotice.setLang(locale.getLanguage());
		tslLegalNotice.setValue(legalNotice);

		tslLegalNotices.add(tslLegalNotice);
	}

	public String getLegalNotice() {
		return getLegalNotice(Locale.ENGLISH);
	}

	public String getLegalNotice(Locale locale) {
		if (null == this.trustStatusList) {
			return null;
		}
		TSLSchemeInformationType schemeInformation = this.trustStatusList
				.getSchemeInformation();
		if (null == schemeInformation) {
			return null;
		}
		PolicyOrLegalnoticeType policyOrLegalnotice = schemeInformation
				.getPolicyOrLegalNotice();
		if (null == policyOrLegalnotice) {
			return null;
		}
		List<MultiLangStringType> tslLegalNotices = policyOrLegalnotice
				.getTSLLegalNotice();
		for (MultiLangStringType tslLegalNotice : tslLegalNotices) {
			String lang = tslLegalNotice.getLang();
			if (locale.getLanguage().equals(lang)) {
				return tslLegalNotice.getValue();
			}
		}
		return null;
	}

	public void setHistoricalInformationPeriod(int historicalInformationPeriod) {
		TSLSchemeInformationType schemeInformation = getSchemeInformation();
		schemeInformation.setHistoricalInformationPeriod(BigInteger
				.valueOf(historicalInformationPeriod));
	}

	public Integer getHistoricalInformationPeriod() {
		if (null == this.trustStatusList) {
			return null;
		}
		TSLSchemeInformationType schemeInformation = this.trustStatusList
				.getSchemeInformation();
		if (null == schemeInformation) {
			return null;
		}
		BigInteger historicalInformationPeriod = schemeInformation
				.getHistoricalInformationPeriod();
		if (null == historicalInformationPeriod) {
			return null;
		}
		return historicalInformationPeriod.intValue();
	}

	public void setListIssueDateTime(DateTime listIssueDateTime) {
		TSLSchemeInformationType schemeInformation = getSchemeInformation();
		GregorianCalendar listIssueCalendar = listIssueDateTime
				.toGregorianCalendar();
		listIssueCalendar.setTimeZone(TimeZone.getTimeZone("Z"));
		schemeInformation.setListIssueDateTime(this.datatypeFactory
				.newXMLGregorianCalendar(listIssueCalendar));
	}

	public DateTime getListIssueDateTime() {
		if (null == this.trustStatusList) {
			return null;
		}
		TSLSchemeInformationType schemeInformation = this.trustStatusList
				.getSchemeInformation();
		if (null == schemeInformation) {
			return null;
		}
		XMLGregorianCalendar listIssueDateTime = schemeInformation
				.getListIssueDateTime();
		if (null == listIssueDateTime) {
			return null;
		}
		GregorianCalendar listIssueCalendar = listIssueDateTime
				.toGregorianCalendar();
		DateTime dateTime = new DateTime(listIssueCalendar);
		return dateTime;
	}

	public void setNextUpdate(DateTime nextUpdateDateTime) {
		TSLSchemeInformationType schemeInformation = getSchemeInformation();
		GregorianCalendar nextUpdateCalendar = nextUpdateDateTime
				.toGregorianCalendar();
		nextUpdateCalendar.setTimeZone(TimeZone.getTimeZone("Z"));

		NextUpdateType nextUpdate = schemeInformation.getNextUpdate();
		if (null == nextUpdate) {
			nextUpdate = this.objectFactory.createNextUpdateType();
			schemeInformation.setNextUpdate(nextUpdate);
		}
		nextUpdate.setDateTime(this.datatypeFactory
				.newXMLGregorianCalendar(nextUpdateCalendar));
	}

	public DateTime getNextUpdate() {
		if (null == this.trustStatusList) {
			return null;
		}
		TSLSchemeInformationType schemeInformation = this.trustStatusList
				.getSchemeInformation();
		if (null == schemeInformation) {
			return null;
		}

		NextUpdateType nextUpdate = schemeInformation.getNextUpdate();
		if (null == nextUpdate) {
			return null;
		}
		XMLGregorianCalendar nextUpdateXmlCalendar = nextUpdate.getDateTime();
		DateTime nextUpdateDateTime = new DateTime(nextUpdateXmlCalendar
				.toGregorianCalendar());
		return nextUpdateDateTime;
	}

	public void addTrustServiceProvider(
			TrustServiceProvider trustServiceProvider) {
		TrustStatusListType trustStatusList = getTrustStatusList();
		TrustServiceProviderListType trustServiceProviderList = trustStatusList
				.getTrustServiceProviderList();
		if (null == trustServiceProviderList) {
			trustServiceProviderList = this.objectFactory
					.createTrustServiceProviderListType();
			trustStatusList
					.setTrustServiceProviderList(trustServiceProviderList);
		}
		List<TSPType> tspList = trustServiceProviderList
				.getTrustServiceProvider();
		tspList.add(trustServiceProvider.getTSP());
		// reset Java model cache
		this.trustServiceProviders = null;
	}

	private static final int BORDER = 0;

	public void humanReadableExport(File pdfExportFile) {
		com.lowagie.text.Document document = new com.lowagie.text.Document();
		OutputStream outputStream;
		try {
			outputStream = new FileOutputStream(pdfExportFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("file not found: "
					+ pdfExportFile.getAbsolutePath(), e);
		}
		try {
			PdfWriter.getInstance(document, outputStream);
			document.open();

			// title
			String title = getSourceLanguageShortName() + ": Trusted List";
			Paragraph titleParagraph = new Paragraph(title);
			titleParagraph.setAlignment(Paragraph.ALIGN_CENTER);
			Font titleFont = titleParagraph.getFont();
			titleFont.setSize((float) 30.0);
			titleFont.setStyle(Font.BOLD);
			titleParagraph.setSpacingAfter(20);
			document.add(titleParagraph);

			Phrase footerPhrase = new Phrase("PDF document generated on "
					+ new Date().toString() + ", page ");
			HeaderFooter footer = new HeaderFooter(footerPhrase, true);
			document.setFooter(footer);

			Phrase headerPhrase = new Phrase(title);
			HeaderFooter header = new HeaderFooter(headerPhrase, false);
			document.setHeader(header);

			Paragraph schemeNameTitle = new Paragraph("Scheme name");
			Font schemeNameTitleFont = schemeNameTitle.getFont();
			schemeNameTitleFont.setStyle(Font.BOLD);
			schemeNameTitle.setFont(schemeNameTitleFont);
			document.add(schemeNameTitle);
			document.add(new Paragraph(this.getSchemeName()));

			Paragraph legalNoticeTitle = new Paragraph("Legal Notice");
			Font legalNoticeTitleFont = legalNoticeTitle.getFont();
			legalNoticeTitleFont.setStyle(Font.BOLD);
			legalNoticeTitle.setFont(legalNoticeTitleFont);
			document.add(legalNoticeTitle);
			document.add(new Paragraph(this.getLegalNotice()));

			// information table
			PdfPTable informationTable = new PdfPTable(2);
			informationTable.getDefaultCell().setBorder(BORDER);
			informationTable.addCell("Scheme territory");
			informationTable.addCell(this.getSchemeTerritory());
			informationTable.addCell("Scheme status determination approach");
			informationTable.addCell(this.getStatusDeterminationApproach()
					.substring(
							this.getStatusDeterminationApproach().indexOf(
									"StatusDetn/")
									+ "StatusDetn/".length()));
			informationTable.addCell("Scheme type community rules");
			PdfPCell schemeTypeCell = new PdfPCell();
			schemeTypeCell.setBorder(0);
			for (String schemeType : this.getSchemeTypes()) {
				schemeType = schemeType.substring(schemeType
						.indexOf("schemerules/")
						+ "schemerules/".length());
				schemeTypeCell.addElement(new Paragraph(schemeType));
			}
			informationTable.addCell(schemeTypeCell);

			informationTable.addCell("Issue date");
			informationTable.addCell(this.getListIssueDateTime().toString());
			informationTable.addCell("Next update");
			informationTable.addCell(this.getNextUpdate().toString());
			informationTable.addCell("Historical information period");
			informationTable.addCell(this.getHistoricalInformationPeriod()
					.toString()
					+ " days");
			informationTable.addCell("Sequence number");
			informationTable.addCell(this.getSequenceNumber().toString());
			informationTable.addCell("Scheme information URIs");
			PdfPCell schemeInfoCell = new PdfPCell();
			schemeInfoCell.setBorder(0);
			for (String schemeInformationUri : this.getSchemeInformationUris()) {
				schemeInfoCell.addElement(new Paragraph(schemeInformationUri));
			}
			informationTable.addCell(schemeInfoCell);

			document.add(informationTable);

			Paragraph schemeOperatorTitle = new Paragraph("Scheme Operator",
					new Font(Font.HELVETICA, 18, Font.BOLDITALIC));
			schemeOperatorTitle.setAlignment(Paragraph.ALIGN_CENTER);
			document.add(schemeOperatorTitle);

			informationTable = new PdfPTable(2);
			informationTable.getDefaultCell().setBorder(BORDER);
			informationTable.addCell("Scheme operator name");
			informationTable.addCell(this.getSchemeOperatorName());
			PostalAddressType schemeOperatorPostalAddress = this
					.getSchemeOperatorPostalAddress(Locale.ENGLISH);
			informationTable.addCell("Scheme operator street address");
			informationTable.addCell(schemeOperatorPostalAddress
					.getStreetAddress());
			informationTable.addCell("Scheme operator postal code");
			informationTable.addCell(schemeOperatorPostalAddress
					.getPostalCode());
			informationTable.addCell("Scheme operator locality");
			informationTable.addCell(schemeOperatorPostalAddress.getLocality());
			informationTable.addCell("Scheme operator state");
			informationTable.addCell(schemeOperatorPostalAddress
					.getStateOrProvince());
			informationTable.addCell("Scheme operator country");
			informationTable.addCell(schemeOperatorPostalAddress
					.getCountryName());

			List<String> schemeOperatorElectronicAddressess = this
					.getSchemeOperatorElectronicAddresses();
			informationTable.addCell("Scheme operator contact");
			PdfPCell schemeAddressInfoCell = new PdfPCell();
			schemeAddressInfoCell.setBorder(0);
			for (String schemeOperatorElectronicAddress : schemeOperatorElectronicAddressess) {
				schemeAddressInfoCell.addElement(new Paragraph(
						schemeOperatorElectronicAddress));
			}
			informationTable.addCell(schemeAddressInfoCell);

			document.add(informationTable);

			Paragraph serviceProvidersTitle = new Paragraph(
					"Trust Service Providers", new Font(Font.HELVETICA, 18,
							Font.BOLDITALIC));
			serviceProvidersTitle.setAlignment(Paragraph.ALIGN_CENTER);
			document.add(serviceProvidersTitle);

			List<TrustServiceProvider> trustServiceProviders = this
					.getTrustServiceProviders();
			for (TrustServiceProvider trustServiceProvider : trustServiceProviders) {
				document.add(new Paragraph(trustServiceProvider.getName(),
						new Font(Font.HELVETICA, 18, Font.BOLD)));
				PdfPTable providerTable = new PdfPTable(2);
				providerTable.getDefaultCell().setBorder(BORDER);
				providerTable.addCell("Service provider trade name");
				providerTable.addCell(trustServiceProvider.getTradeName());
				providerTable.addCell("Information URI");
				providerTable.addCell(trustServiceProvider.getInformationUri());
				PostalAddressType postalAddress = trustServiceProvider
						.getPostalAddress();
				providerTable.addCell("Service provider street address");
				providerTable.addCell(postalAddress.getStreetAddress());
				providerTable.addCell("Service provider postal code");
				providerTable.addCell(postalAddress.getPostalCode());
				providerTable.addCell("Service provider locality");
				providerTable.addCell(postalAddress.getLocality());
				providerTable.addCell("Service provider state");
				providerTable.addCell(postalAddress.getStateOrProvince());
				providerTable.addCell("Service provider country");
				providerTable.addCell(postalAddress.getCountryName());
				document.add(providerTable);

				List<TrustService> trustServices = trustServiceProvider
						.getTrustServices();
				for (TrustService trustService : trustServices) {
					document.add(new Paragraph(trustService.getName(),
							new Font(Font.HELVETICA, 12, Font.BOLDITALIC)));
					PdfPTable serviceTable = new PdfPTable(2);
					serviceTable.getDefaultCell().setBorder(BORDER);
					serviceTable.addCell("Type");
					serviceTable.addCell(trustService.getType().substring(
							trustService.getType().indexOf("Svctype/")
									+ "Svctype/".length()));
					serviceTable.addCell("Status");
					serviceTable.addCell(trustService.getStatus().substring(
							trustService.getStatus().indexOf("Svcstatus/")
									+ "Svcstatus/".length()));
					serviceTable.addCell("Status starting time");
					serviceTable.addCell(trustService.getStatusStartingTime()
							.toString());
					document.add(serviceTable);
					document.add(new Paragraph(
							"Service digital identity (X509)", new Font(
									Font.HELVETICA, 12, Font.ITALIC)));
					X509Certificate certificate = trustService
							.getServiceDigitalIdentity();
					PdfPTable serviceIdentityTable = new PdfPTable(2);
					serviceIdentityTable.getDefaultCell().setBorder(BORDER);
					serviceIdentityTable.addCell("Subject");
					serviceIdentityTable.addCell(certificate
							.getSubjectX500Principal().toString());
					serviceIdentityTable.addCell("Issuer");
					serviceIdentityTable.addCell(certificate
							.getIssuerX500Principal().toString());
					serviceIdentityTable.addCell("Not before");
					serviceIdentityTable.addCell(certificate.getNotBefore()
							.toString());
					serviceIdentityTable.addCell("Not after");
					serviceIdentityTable.addCell(certificate.getNotAfter()
							.toString());
					serviceIdentityTable.addCell("Serial number");
					serviceIdentityTable.addCell(certificate.getSerialNumber()
							.toString());
					serviceIdentityTable.addCell("Version");
					serviceIdentityTable.addCell(Integer.toString(certificate
							.getVersion()));
					byte[] encodedCertificate;
					try {
						encodedCertificate = certificate.getEncoded();
					} catch (CertificateEncodingException e) {
						throw new RuntimeException("cert: " + e.getMessage(), e);
					}
					serviceIdentityTable.addCell("SHA1 Thumbprint");
					String thumbprint = DigestUtils.shaHex(encodedCertificate);
					serviceIdentityTable.addCell(thumbprint);
					serviceIdentityTable.addCell("SHA256 Thumbprint");
					String sha256thumbprint = DigestUtils
							.sha256Hex(encodedCertificate);
					serviceIdentityTable.addCell(sha256thumbprint);
					document.add(serviceIdentityTable);

					document.add(new Paragraph("The decoded certificate:"));
					Paragraph certParagraph = new Paragraph(certificate
							.toString(), new Font(Font.COURIER, 8, Font.NORMAL));
					// certParagraph.setAlignment(Paragraph.ALIGN_CENTER);
					document.add(certParagraph);

					document
							.add(new Paragraph("The certificate in PEM format:"));
					Paragraph pemParagraph = new Paragraph(toPem(certificate),
							new Font(Font.COURIER, 8, Font.NORMAL));
					pemParagraph.setAlignment(Paragraph.ALIGN_CENTER);
					document.add(pemParagraph);
				}
			}

			X509Certificate signerCertificate = verifySignature();
			if (null != signerCertificate) {
				Paragraph tslSignerTitle = new Paragraph("TSL Signer",
						new Font(Font.HELVETICA, 18, Font.BOLDITALIC));
				tslSignerTitle.setAlignment(Paragraph.ALIGN_CENTER);
				document.add(tslSignerTitle);

				PdfPTable signerTable = new PdfPTable(2);
				signerTable.getDefaultCell().setBorder(BORDER);
				signerTable.addCell("Subject");
				signerTable.addCell(signerCertificate.getSubjectX500Principal()
						.toString());
				signerTable.addCell("Issuer");
				signerTable.addCell(signerCertificate.getIssuerX500Principal()
						.toString());
				signerTable.addCell("Not before");
				signerTable
						.addCell(signerCertificate.getNotBefore().toString());
				signerTable.addCell("Not after");
				signerTable.addCell(signerCertificate.getNotAfter().toString());
				signerTable.addCell("Serial number");
				signerTable.addCell(signerCertificate.getSerialNumber()
						.toString());
				signerTable.addCell("Version");
				signerTable.addCell(Integer.toString(signerCertificate
						.getVersion()));
				byte[] encodedPublicKey = signerCertificate.getPublicKey()
						.getEncoded();
				signerTable.addCell("Public key SHA1 Thumbprint");
				String thumbprint = DigestUtils.shaHex(encodedPublicKey);
				signerTable.addCell(thumbprint);
				signerTable.addCell("Public key SHA256 Thumbprint");
				String sha256thumbprint = DigestUtils
						.sha256Hex(encodedPublicKey);
				signerTable.addCell(sha256thumbprint);
				document.add(signerTable);

				document.add(new Paragraph("The decoded certificate:"));
				Paragraph certParagraph = new Paragraph(signerCertificate
						.toString(), new Font(Font.COURIER, 8, Font.NORMAL));
				// certParagraph.setAlignment(Paragraph.ALIGN_CENTER);
				document.add(certParagraph);

				document.add(new Paragraph("The certificate in PEM format:"));
				Paragraph pemParagraph = new Paragraph(
						toPem(signerCertificate), new Font(Font.COURIER, 8,
								Font.NORMAL));
				pemParagraph.setAlignment(Paragraph.ALIGN_CENTER);
				document.add(pemParagraph);

				document.add(new Paragraph("The public key in PEM format:"));
				Paragraph publicKeyPemParagraph = new Paragraph(
						toPem(signerCertificate.getPublicKey()), new Font(
								Font.COURIER, 8, Font.NORMAL));
				publicKeyPemParagraph.setAlignment(Paragraph.ALIGN_CENTER);
				document.add(publicKeyPemParagraph);
			}

			document.close();
		} catch (DocumentException e) {
			throw new RuntimeException("PDF document error: " + e.getMessage(),
					e);
		}
	}

	/**
	 * http://publications.europa.eu/code/pdf/370000en.htm
	 * 
	 * @return
	 */
	private String getSourceLanguageShortName() {
		String countryCode = getSchemeTerritory();
		if ("BE".equals(countryCode)) {
			return "Belgique/België (Belgium)";
		}
		return countryCode;
	}

	private String toPem(Object object) {
		StringWriter buffer = new StringWriter();
		try {
			PEMWriter writer = new PEMWriter(buffer);
			writer.writeObject(object);
			writer.close();
			return buffer.toString();
		} catch (Exception e) {
			throw new RuntimeException(
					"Cannot convert public key to PEM format: "
							+ e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(buffer);
		}
	}

	public void addDistributionPoint(String distributionPointUri) {
		TSLSchemeInformationType schemeInformation = getSchemeInformation();
		ElectronicAddressType distributionPoints = schemeInformation
				.getDistributionPoints();
		if (null == distributionPoints) {
			distributionPoints = this.objectFactory
					.createElectronicAddressType();
			schemeInformation.setDistributionPoints(distributionPoints);
		}
		List<String> uris = distributionPoints.getURI();
		uris.add(distributionPointUri);
	}

	public String getSha1Fingerprint() {
		String fingerprint = DigestUtils.shaHex(toByteArray());
		return fingerprint;
	}

	private byte[] toByteArray() throws TransformerFactoryConfigurationError {
		Source source = new DOMSource(this.tslDocument);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		Result result = new StreamResult(byteArrayOutputStream);
		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer;
		try {
			transformer = transformerFactory.newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new RuntimeException(e);
		}
		// transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
		// "yes");
		try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
		return byteArrayOutputStream.toByteArray();
	}

	public String getSha256Fingerprint() {
		String fingerprint = DigestUtils.sha256Hex(toByteArray());
		return fingerprint;
	}
}