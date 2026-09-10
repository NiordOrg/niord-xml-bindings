# Creating S-124 exchange sets

[Project overview](../README.md) · [Using the bindings](usage.md) · [Development](development.md)

An exchange set packages datasets together with a discovery catalogue and digital
signatures. `S124ExchangeSetFactory` builds the complete ZIP in memory. You supply
the datasets, producer metadata, certificate and signing callback; the factory
handles serialization, catalogue construction and packaging.

## Build a ZIP

This helper accepts populated datasets, the Base64 body of a Data Server
certificate and an application-provided signer:

```java
import java.util.List;

import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets.S124ExchangeSetFactory;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets.S124Signer;

public class ExchangeSetExample {
    public static byte[] create(List<Dataset> datasets, String certificatePem,
                                S124Signer signer) {
        return S124ExchangeSetFactory.builder()
                .datasets(datasets)
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .emails(List.of("warnings@example.org"))
                .certificatePem(certificatePem)
                .signer(signer)
                .description("Example S-124 navigational warnings")
                .notForNavigation(true)
                .build()
                .toBytes();
    }
}
```

Write the returned bytes with `Files.write(path, zipBytes)`. Use your own
organization, contact details and registered producer code in an integration. At least one
dataset or cancellation is required. The producer code must contain exactly
four alphanumeric characters.

Producers whose datasets may later have to be withdrawn should call `toExchangeSet()`
instead. It returns the same ZIP plus the catalogue entry published for each dataset,
which is what a later cancellation has to reproduce; see
[Cancel a previously delivered dataset](#cancel-a-previously-delivered-dataset).

Producer contact information is mandatory for dataset metadata. Supply an email
address, phone number, postal address fields, `onlineResource(...)` or
`contactInstructions(...)`; the example email above is a placeholder.

Each dataset must have a usable bounding envelope or member geometry from which
the factory can calculate its extent. A geometry-free in-force bulletin therefore
needs an explicit envelope. The example generator demonstrates that case.

## Supply a signer

`S124Signer.sign(algorithm, payload)` receives the exact bytes to sign and returns
the signature. The factory invokes it for each dataset and for the final
catalogue. Key storage and signing are the application's responsibility; a
keystore, HSM or signing service can implement the callback.

The builder defaults to `ECDSA_384_SHA_2` and rejects other algorithms.

### Signature encoding

Return the signature as S-100 Part 15, clause 15-8.4, embeds it: the ASN.1 DER
`SEQUENCE` of the two ECDSA integers r and s, **not** Base64 encoded. That is what
Java's `SHA384withECDSA` returns and what OpenSSL produces:

```java
S124Signer signer = (algorithm, payload) -> {
    Signature ecdsa = Signature.getInstance("SHA384withECDSA");
    ecdsa.initSign(privateKey);
    ecdsa.update(payload);
    return ecdsa.sign();
};
```

The factory performs no conversion. JAXB applies the single Base64 layer when it
writes the `xs:base64Binary` signature elements. A value that is not a DER sequence
of two integers is rejected with `ExchangeSetException` before anything is
packaged. The common mistake is the raw 96-byte `r||s` form produced by
`SHA384withECDSAinP1363Format`: it verifies in your own code, but no Part 15
reader can decode it.

The repository's tests generate throwaway P-384 keys and certificates in the JVM
(`SigningIdentityFixture`, BouncyCastle in test scope only) and verify real signatures
end to end. The
[example generator](../s-124/src/test/java/dk/dma/niord/s100/xmlbindings/s124/v2_0_0/examples/DanishWatersExamplesGenerator.java)
signs with the same class.

Use a signing key that matches `certificatePem`. If a Domain Coordinator issued
the certificate, supply its intermediate certificate chain through
`intermediateCertificatePems(...)`. The factory orders and checks issuer links
cryptographically. The Scheme Administrator root is installed separately by the
consumer. The default administrator ID is `IHO` and can be changed with
`schemeAdministrator(...)`.

Despite the `certificatePem` name, certificate strings must contain **only the
Base64 body**, without PEM header/footer lines or whitespace. This also applies
to intermediate and cancellation certificate chains. To read a certificate file
inside a method:

```java
String certificatePem = java.nio.file.Files.readString(java.nio.file.Path.of("signer-cert.pem"))
        .replaceAll("-----(BEGIN|END) CERTIFICATE-----", "")
        .replaceAll("\\s", "");
```

The factory checks the shape of the callback output, not the key behind it. It is
not a signature verifier or a substitute for consumer trust validation, and a
successful ZIP build alone does not prove the callback used the private key that
matches `certificatePem`.

## ZIP layout

```text
S100_ROOT/
├── CATALOG.XML
├── CATALOG.SIGN
└── S-124/
    ├── DATASET_FILES/
    │   └── 124DK00DKNW01126.GML
    ├── CATALOGUES/
    └── SUPPORT_FILES/
```

`CATALOG.XML` describes the datasets and carries their signatures and certificate
metadata. `CATALOG.SIGN` is an XML `StandaloneDigitalSignature` document signing
the catalogue bytes. The `CATALOGUES` and `SUPPORT_FILES` directories are currently
created empty by this factory.

Dataset filenames follow `124<producer code><alphanumeric unique code>.GML`.
When a header specifies `datasetFileIdentifier`, the factory uses that name and
checks its format and producer prefix. It rejects duplicate filenames and invalid
declared names. Keeping the header and packaged filename aligned matters because
the header is part of the signed payload.

In the catalogue, each dataset is announced as
`file:/S-124/DATASET_FILES/124DK00DKNW01126.GML` — the path the archive packages it
under, relative to `CATALOG.XML`, which S-100 Part 17, clause 17-4.2, puts in
`S100_ROOT`, so the `S100_ROOT/` segment itself is absent.

Nothing mandates this form. The `fileName` element is an unfaceted `xs:anyURI`,
Part 17 states its conformance test as schema validation alone, and the validation
authority S-124 clause 8.11.1 defers to does not constrain it either: of the 58
Part 17 checks in the published S-158:100 Ed 1.0.0 — which S-158:124 adopts
wholesale for S-124 exchange sets — not one bears on this element. It is emitted
for interoperability, and every non-check signal runs the same way:

- The S-124 exchange sets of the [IHO S-164 test data](https://github.com/iho-ohi/S-164-Sub-Group)
  all spell it this way, over a folder layout identical to ours.
- Clients resolve a `fileName` by stripping the scheme and joining the remainder
  onto the directory holding the catalogue, which locates a dataset only when the
  product path is present.
- S-124 Ed 2.0.0, clause 12.2.2, annotates this element — alone among the
  discovery-metadata file names — "including how to capture the path of the file".
- The working draft of S-158:100 Ed 2.0.0 exempts `datasetFileIdentifier` and
  `fileName` from check 100_0324's consistency comparison because
  "datasetFileIdentifier excludes path information and will not be a URI".

That last point is also why the GML header's `datasetFileIdentifier` stays the bare
name, as S-100 Part 10b, Table 10b-4, requires; the two deliberately differ.

## Checks and defaults

Before signing a dataset, the factory completes missing codes, checks the
implemented dataset rules, validates the XML against the bundled S-124 schema
and enforces a **51,200-byte (50 × 1024) limit** on the serialized dataset.
The factory uses formatted dataset XML, so whitespace counts toward that limit.

`validateAgainstSchema(false)` disables the dataset XSD check only. It does not
disable the dataset-rule validator or the size limit. Keep the default for
normal exchange-set creation.

Catalogue XSD validation is exercised in the repository's tests and generator;
the production factory does not run it. See [validation details](usage.md#understand-the-two-validation-layers).

| Setting | Default or behavior |
| --- | --- |
| `notForNavigation` | `true`; set deliberately for the intended delivery |
| Product specification | Navigational Warnings, S-124 2.0.0, category 3 |
| `specificUsage` | `Navigational Warning Service`; `null` omits it, other values are rejected |
| Classification | Unclassified |
| Locale | English |
| Exchange-set identifier | `urn:mrn:iho:s124:exchangeset:<random UUID>` |
| Dataset identifier | Preamble interoperability identifier, or `urn:mrn:iho:s124:<dataset gml:id>` |

Dataset identifiers must be Marine Resource Names (MRNs). A custom
`datasetMrnPrefix(...)` changes the fallback prefix. The factory derives geographic
and temporal metadata from each dataset: it pads point/line extents into bounding
boxes with positive spans and includes a temporal extent when the preamble has
a cancellation date. The catalogue description for a dataset comes from its
preamble's general area and locality.

`S124ConformanceException` reports dataset-rule failures with clause references.
`S124ExchangeSetFactory.ExchangeSetException` reports packaging, schema,
certificate and other exchange-set failures. Invalid or missing builder settings
can fail earlier with `IllegalArgumentException` or `NullPointerException`.

## Cancel a previously delivered dataset

A fileless cancellation is a catalogue entry that tells a consumer to remove a
previous dataset. It retains the original filename, signature and mandatory
metadata, sets its purpose to cancellation and ships no replacement dataset file.

The filename is retained exactly as the original entry carried it, because clause
17-4.4.1 has the consumer match the cancellation against the record it already
holds. A dataset published before 0.3.1 was announced by its bare name and is
withdrawn under that same string, never rewritten into the path form. A catalogue
that cancels an old dataset while publishing a new one therefore carries both
spellings; that is the correct output, not a defect.

Create `new S124ExchangeSetFactory.Cancellation(originalMetadata, issueDate)` and
pass the entries to `builder.cancellations(...)`. A cancellation-only
set still needs the organization, producer code, certificate and signer because
the new catalogue must be signed.

### Capture the entry when you publish

`originalMetadata` is the entry the original catalogue published, so capture it at
publish time and store it:

```java
// publish
S124ExchangeSetFactory.ExchangeSet set = factory.toExchangeSet();
Files.write(path, set.bytes());
S124ExchangeSetFactory.PublishedDataset published = set.datasets().get(0);
row.setFileName(published.fileName());
row.setDiscoveryMetadata(
        S124ExchangeSetFactory.discoveryMetadataToXml(published.discoveryMetadata()));
row.setSignatureCertificates(String.join("\n", set.signingCertificatePems()));

// cancel, months and one certificate rotation later
var cancellation = new S124ExchangeSetFactory.Cancellation(
        S124ExchangeSetFactory.discoveryMetadataFromXml(row.getDiscoveryMetadata()),
        LocalDate.now(ZoneOffset.UTC),
        List.of(row.getSignatureCertificates().split("\n")));
```

`set.datasets()` has one entry per dataset, at the index that dataset has in
`datasets(...)`, and `published.dataset()` is the object you passed in — the file
name is not predictable at publish time, so join on the dataset rather than on the
name. `published.fileName()` is the bare `124….GML` name, deliberately not the
catalogue URI, so the column above needs no migration when the catalogue's spelling
of it changes.

**What to store.** The entry is a schema-defined `S100_DatasetDiscoveryMetadata`
document of roughly 4.5 kB, so the column must be CLOB/TEXT, not VARCHAR. It
declares and is decoded as UTF-8, so the column has to hold the full repertoire —
a Latin-1 column will not round-trip the free text (`producingAgency`, the
description and the comment) that clause 17-4.4.1 makes the cancellation reproduce.
Never
hash it or use it as an idempotency key: JAXB chooses the namespace prefixes, so
the exact bytes may differ across versions for an identical entry — compare the
parsed entries instead. Always store the signing chain too, and always pass it
rather than relying on the empty `certificatePems` default: the entry records only
a catalogue-scoped certificate id, and the default assumes the certificate that
signed the original is still the current one. `certificatePems` is a list because
a domain-coordinator chain has more than one element; the newline join above is one
workable single-column convention.

The two-argument constructor assumes the original signature used the current
Data Server certificate. If the signing certificate has changed, use the overload
that also accepts the original certificate chain, signing certificate first.
For counter-signed originals, the full constructor additionally accepts a map
of counter-signer chains keyed by their original `certificateRef` values. This
allows the factory to preserve signature chains and update certificate references
within the new catalogue.

Retain the original catalogue entry and the certificate chain that signed it
together — `ExchangeSet` hands both over from the same build. The factory copies
the entry and never modifies it, so the same record can be reused in later
exchange sets under whichever certificate is then current. Passing the current
certificate as the original chain is harmless: identical certificates are carried
once, and the reused signature references the current entry.

### Serve a retained dataset verbatim

A warning stays in force for months, and every exchange set that serves it must carry
the one signature its cancellation will later be matched by (clause 17-4.4.1). The
simplest way to guarantee that is to keep what the publishing build handed over and
never marshal the dataset again: `published.bytes()` is the packaged file, the entry is
the catalogue record, and `set.signingCertificatePems()` is the chain that signed it.
Hand the three back as a `RetainedDataset`:

```java
// publish once: keep the bytes beside the entry and the chain
S124ExchangeSetFactory.ExchangeSet set = factory.toExchangeSet();
S124ExchangeSetFactory.PublishedDataset published = set.datasets().get(0);
row.setGml(new String(published.bytes(), StandardCharsets.UTF_8));
row.setDiscoveryMetadata(
        S124ExchangeSetFactory.discoveryMetadataToXml(published.discoveryMetadata()));
row.setSignatureCertificates(String.join("\n", set.signingCertificatePems()));

// serve it again, any number of times, under whichever certificate is then current
S124ExchangeSetFactory.builder()
        .retainedDatasets(List.of(new S124ExchangeSetFactory.RetainedDataset(
                row.getGml().getBytes(StandardCharsets.UTF_8),
                S124ExchangeSetFactory.discoveryMetadataFromXml(row.getDiscoveryMetadata()),
                List.of(row.getSignatureCertificates().split("\n")))))
        .certificatePem(currentPem)
        .signer(currentSigner)
        // ...
        .build()
        .toBytes();
```

The bytes are packaged as they are, without being parsed, validated or re-marshalled.
The entry is reproduced whole - purpose, issue date and metadata date stamp included,
because it describes the publication the consumer already holds and is the entry a later
cancellation reproduces - with only the signature's `certificateRef` re-labelled to the
id this catalogue carries the certificate under. After a rotation the chain travels
along, exactly as for a cancellation; passing the current certificate is harmless. The
catalogue itself is always signed by the current certificate, and `set.datasets()` of the
new build lists only the freshly published datasets, since a retained one yields no new
record.

Retained datasets are packaged after the datasets of `datasets(...)` and before any
cancellation, and share their file-name uniqueness rule (clause 17-4.3) and the clause 9.6
size limit. An entry whose signature was counter-signed cannot be retained this way;
publish such a dataset afresh. New in 0.3.3.

### Ship a dataset again under its original signature

A producer that keeps the typed dataset rather than the packaged bytes - and so
re-marshals it on every build - can still serve the one signature clause 17-4.4.1 requires,
for as long as the marshalled form stays byte-identical. Hand the kept signature back
through `reusedSignatures(...)`, keyed by `S124ExchangeSetFactory.payloadHash(...)` of the
dataset file bytes, together with the chain that made it (empty when the current
certificate did). Prefer a [retained dataset](#serve-a-retained-dataset-verbatim) where the
bytes can be kept: it needs no hash, no re-marshal and no assumption about the bindings
version that serves it.

```java
// serve the dataset again, possibly under a later certificate
S124ExchangeSetFactory.builder()
        .datasets(List.of(dataset))
        .reusedSignatures(Map.of(
                row.getSignedPayloadHash(),          // payloadHash(...) of the file bytes, kept at publish
                                                     // time - S124Utils.marshalS124(dataset) as UTF-8 is
                                                     // what the factory packages and signs
                new S124ExchangeSetFactory.ReusedSignature(
                        row.getSignature(),
                        List.of(row.getSignatureCertificates().split("\n")))))
        .certificatePem(currentPem)
        .signer(currentSigner)
        // ...
        .build()
        .toBytes();
```

The factory embeds the signature unchanged instead of asking the signer, and points it at
the certificate that made it - carried under an id of the catalogue's own when it is not
the current one, exactly as for a cancellation. A dataset whose bytes hash to no key is
signed afresh, so a signature kept for an earlier version is never served for changed
content; a reused value is held to the same clause 15-8.4 form as a fresh one. The
catalogue itself is always signed by the current certificate. Passing the current
certificate as the chain is harmless: identical certificates are carried once. New in
0.3.2; nothing changes for callers that do not use it.

### Back-fill from an archived exchange set

`S124ExchangeSetFactory.readDiscoveryMetadata(zipBytes)` returns the
`purpose=newDataset` entries of an already-built exchange set, keyed by the
`124….GML` file name a producer already stores — the last path segment of the
catalogue's `xs:anyURI` value, with any URI scheme removed. Keying on that rather
than on the value whole is what makes the lookup work across sets written before
and after 0.3.1, and across foreign producers who write a path, Windows separators
or no scheme at all.

Use it for warnings published before `toExchangeSet()` existed: the shipped
catalogue is the only faithful record of the original, and the entry must be
reproduced unchanged. It is not the publish-time path — it re-parses what the
factory had in hand, and it can only tell datasets apart by file name.

The catalogue is parsed as foreign XML: `SecureXmlSource` refuses a `<!DOCTYPE>`
outright, so an external entity cannot read a file off the reading host or make it
issue a request, and a billion-laughs expansion cannot be declared. A conformant
catalogue never carries a DOCTYPE (S-100 Part 17, clause 17-4.2), so nothing
legitimate is refused; one that does fails as an `ExchangeSetException`. The same
applies to `discoveryMetadataFromXml(...)`, `S124Utils.unmarshallS124(...)`,
`S124Utils.prettyPrint(...)` and `S124XsdValidator.validate(...)`.

What is **not** covered: the ZIP is not size-bounded. `CATALOG.XML` is read with
`readAllBytes()`, so a decompression bomb still exhausts the heap of whatever reads
it. Bound the archive where you accept it — only you know what a legitimate exchange
set weighs in your deployment.

### Migrating from 0.3.0

The catalogue now announces each dataset as
`file:/S-124/DATASET_FILES/124….GML` instead of `file:/124….GML`. Only the emitted
XML changes: the Java API is source- and binary-compatible, `published.fileName()`
is still the bare name, and no stored row needs migrating. Cancellations of
datasets published before this release keep the name they were published under, by
design (see above).

This was not a conformance defect — the element is an unfaceted `xs:anyURI` — but
the bare form does not resolve for a client that joins the value onto the
catalogue's directory, which is what the surveyed S-100 clients do and what the
IHO S-164 S-124 test data assumes.

One thing to check before upgrading: if your consumer compensates today by
prepending `S-124/DATASET_FILES/` to the bare name itself, it will double the path
and must stop doing so.

### Migrating from 0.0.12

`Cancellation` used to take the file name, identifiers, bounding box and a list of
signature objects whose `certificateRef` the caller had to set to the factory's
internal `cer1`. That constructor is gone, and it never survived a certificate
rotation. Pass the retained `S100DatasetDiscoveryMetadata` entry instead, together
with the chain that signed it. The retained entry comes from `toExchangeSet()` at
publish time, or from `readDiscoveryMetadata(...)` for sets already published; the
factory allocates every certificate id and rewrites the references itself. Signers
must return DER-encoded signatures (see above); the factory now rejects any other
form.

See the [exchange-set tests](../s-124/src/test/java/dk/dma/niord/s100/xmlbindings/s124/v2_0_0/exchangesets/S124ExchangeSetFactoryTest.java)
for cancellation, rollover and certificate-chain examples.
