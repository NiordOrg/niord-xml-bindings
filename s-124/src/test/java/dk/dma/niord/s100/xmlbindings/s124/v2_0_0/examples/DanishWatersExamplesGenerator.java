package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.examples;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.grad.eNav.s100.utils.S100ExchangeSetUtils;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import dk.dma.niord.s100.catalog._5_2.S100DatasetDiscoveryMetadata;
import dk.dma.niord.s100.catalog._5_2.S100ExchangeCatalogue;
import dk.dma.niord.s100.catalog._5_2.S100Purpose;
import dk.dma.niord.s100.catalog._5_2.S100SEDigitalSignatureReference;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.CurveProperty;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.PointProperty;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.S100SpatialAttributeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.SurfaceProperty;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.DataSetIdentificationTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.ReferenceType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.BoundingShapeTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.EnvelopeTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.PosImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.ReferenceTypeImpl;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.AffectedChartPublicationsType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.FeatureNameType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.FixedDateRangeType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.GeneralAreaType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.InformationType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.LocalityType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.LocationNameType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.MessageSeriesIdentifierType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnPart;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnPreamble;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnTitleType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnTypeGeneralLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnTypeGeneralType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.ObjectFactory;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.QualityOfHorizontalMeasurementLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.QualityOfHorizontalMeasurementType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.SpatialQuality;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.ReferenceCategoryLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.References;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.RestrictionLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.RestrictionType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.WarningInformationType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.WarningTypeLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets.S124ExchangeSetFactory;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets.S124Signer;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets.SigningIdentityFixture;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.GeometryS124Converter;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.S124Utils;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.S124XsdValidator;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.AbstractGMLType;

/**
 * Generates example S-124 v2.0.0 navigational warning datasets for Danish waters and
 * S-100 Part 17 exchange sets containing the same warnings, plus a worked example of how a
 * published warning is later withdrawn: a cancellation dataset (S-124 clause 9.3) delivered
 * together with a fileless cancellation of the original dataset (S-100 Part 17, clause
 * 17-4.4.1).
 *
 * <p>NOT part of the regular test suite (does not match surefire's default includes).
 * Run explicitly with:</p>
 * <pre>
 *   mvn -pl s-124 test -Dtest=DanishWatersExamplesGenerator
 * </pre>
 *
 * <p>Output goes to {@code target/danish-nw-examples/} at the repository root (gitignored).
 * Every dataset is validated against the S-124 2.0.0 XSD and every exchange set catalogue
 * against the S-100 5.2.0 exchange catalogue XSD before it is written.</p>
 *
 * <p>Exchange sets are signed with a throwaway EC P-384 key and self-signed certificate
 * generated in the JVM for the run ({@link SigningIdentityFixture}): real ECDSA signatures,
 * verifiable against the certificate written next to the output.</p>
 */
class DanishWatersExamplesGenerator {

    private static final ObjectFactory OF = new ObjectFactory();
    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);
    private static final String AGENCY = "Danish Maritime Authority";
    /** S-62 producer code from the IHO GI Registry, per S-124 clause 4.3.3. */
    private static final String AGENCY_CODE = "DK00";
    private static final String SERIES = "Danish Nav. Warn.";
    private static final int YEAR = 2026;
    private static final int FIRST_WARNING_NUMBER = 11;
    /** The in-force bulletin is itself a numbered message in the series. */
    private static final int BULLETIN_NUMBER = 17;
    /** So is the message that cancels NW 011/26 once the Drogden buoy is back in service. */
    private static final int CANCELLATION_NUMBER = 18;
    /** The day the six warnings and the bulletin were made into datasets. */
    private static final LocalDate DATASET_REFERENCE_DATE = LocalDate.of(2026, 8, 25);
    /** When NW 018/26 is published - a week after the bulletin, which therefore still lists 011/26. */
    private static final OffsetDateTime CANCELLATION_TIME = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);

    /** One example warning: its members plus everything needed for dataset/exchange-set metadata. */
    private record Warning(String compactId, String titleEn, String abstractEn,
                           List<AbstractGMLType> members, Envelope envelope) {}

    @Test
    void generateExamples() throws Exception {
        Path repoRoot = Files.exists(Path.of("../pom.xml")) ? Path.of("..") : Path.of(".");
        Path out = repoRoot.resolve("target/danish-nw-examples");
        Path datasetsDir = Files.createDirectories(out.resolve("datasets"));
        Path exchangeSetsDir = Files.createDirectories(out.resolve("exchange-sets"));
        // Dataset file names derive from the dataset ids, so a renamed example would otherwise
        // leave its previous name behind and the directory would misrepresent what was generated.
        deleteGeneratedFiles(datasetsDir);
        deleteGeneratedFiles(exchangeSetsDir);
        Path signingDir = Files.createDirectories(out.resolve("signing"));
        // The cancellation example goes to a folder of its own, self-contained and with its own
        // README, so it can be handed over as it is. Everything in it is generated, so it is
        // rebuilt from scratch.
        Path cancellationDir = out.resolve("fileless-cancellation");
        deleteTree(cancellationDir);
        Files.createDirectories(cancellationDir);

        SignerBundle signer = createSigner(signingDir);

        List<Warning> warnings = List.of(
                drogdenBuoyUnlit(), greatBeltCableWork(), kattegatDriftingContainers(),
                bornholmFiringExercise(), hornsRevTurbineUnlit(), limfjordenBridgeClosed());

        // One dataset per warning, validated against the S-124 XSD.
        List<Dataset> datasets = new ArrayList<>();
        for (Warning w : warnings) {
            Dataset ds = buildDataset(w, DATASET_REFERENCE_DATE);
            datasets.add(ds);
            writeDataset(datasetsDir, ds);
            System.out.println("dataset OK  " + w.compactId());
        }

        // An In-Force Bulletin (S-124 clause 8.1.2, Table 8-1 and clause 8.1.3): exactly one
        // NavwarnPreamble and one References with referenceCategory 3 (in-force) listing every
        // warning still in force. noMessageOnHand is false because warnings exist, so one
        // messageSeriesIdentifier per warning is mandatory. The bulletin references itself as
        // active (clause 8.1.3) and carries no geometry, NavwarnAreaAffected or TextPlacement.
        Envelope all = new Envelope();
        for (Warning w : warnings) {
            all.expandToInclude(w.envelope());
        }
        Dataset bulletin = buildInForceBulletin(warnings, all);
        writeDataset(datasetsDir, bulletin);
        System.out.println("dataset OK  DK-NW-2026-in-force (in-force bulletin)");

        // One exchange set per warning, one for the bulletin, and a combined set with all seven.
        // toExchangeSet() rather than toBytes(): besides the ZIP it hands back the catalogue
        // entry published for each dataset, which is what a later cancellation must reproduce.
        List<S124ExchangeSetFactory.ExchangeSet> published = new ArrayList<>();
        for (int i = 0; i < warnings.size(); i++) {
            S124ExchangeSetFactory.ExchangeSet set = buildExchangeSet(List.of(datasets.get(i)), List.of(), signer,
                    "S-124 exchange set with Danish navigational warning " + warnings.get(i).compactId());
            validateCatalogue(set.bytes(), repoRoot);
            Files.write(exchangeSetsDir.resolve(warnings.get(i).compactId() + ".zip"), set.bytes());
            published.add(set);
            System.out.println("exchange set OK  " + warnings.get(i).compactId() + ".zip");
        }
        // S-124 clause 9.5: every dataset must be delivered in an exchange set, the bulletin
        // included - it needs its own discovery metadata entry like any other dataset.
        byte[] bulletinZip = buildExchangeSet(List.of(bulletin), List.of(), signer,
                "S-124 exchange set with the Danish in-force bulletin").bytes();
        validateCatalogue(bulletinZip, repoRoot);
        Files.write(exchangeSetsDir.resolve("DK-NW-2026-in-force.zip"), bulletinZip);
        System.out.println("exchange set OK  DK-NW-2026-in-force.zip");

        List<Dataset> allDatasets = new ArrayList<>(datasets);
        allDatasets.add(bulletin);
        byte[] combinedZip = buildExchangeSet(allDatasets, List.of(), signer,
                "S-124 exchange set with all Danish navigational warnings in force").bytes();
        validateCatalogue(combinedZip, repoRoot);
        Files.write(exchangeSetsDir.resolve("DK-NW-2026-all.zip"), combinedZip);
        extractZip(combinedZip, exchangeSetsDir.resolve("DK-NW-2026-all"));
        System.out.println("exchange set OK  DK-NW-2026-all.zip (+ extracted copy)");

        // ------------------------------------------------------------------
        // Cancelling a published warning: S-124 clause 9.3, second bullet - a cancellation
        // dataset "as well as including a fileless cancellation (see S-100 Part 17, clause
        // 17-4.4.1) of the dataset being cancelled". Both travel in one exchange set.
        // ------------------------------------------------------------------
        Warning cancelled = warnings.get(0);
        S124ExchangeSetFactory.ExchangeSet originalSet = published.get(0);
        S124ExchangeSetFactory.PublishedDataset originalEntry = originalSet.datasets().get(0);

        // Step 1 - the exchange set that published the warning, as it was delivered. The same
        // bytes as exchange-sets/DK-NW-011-26.zip; repeated here so the folder stands alone.
        Path originalDir = Files.createDirectories(cancellationDir.resolve("1-original"));
        Files.write(originalDir.resolve(cancelled.compactId() + ".zip"), originalSet.bytes());
        extractZip(originalSet.bytes(), originalDir.resolve(cancelled.compactId()));

        // Step 2 - what the producer keeps when it publishes: the catalogue entry that announced
        // the dataset and the certificate chain that signed it. Clause 17-4.4.1 has the
        // cancellation reproduce the entry "with all other mandatory metadata fields also set to
        // the same values as the original", so it is stored at publish time, not rebuilt later
        // from a configuration that may have moved on. Here it is a file; in Niord, a column.
        Path retainedEntry = Files.createDirectories(cancellationDir.resolve("2-retained"))
                .resolve(cancelled.compactId() + ".discovery-metadata.xml");
        Files.writeString(retainedEntry,
                S124ExchangeSetFactory.discoveryMetadataToXml(originalEntry.discoveryMetadata()));

        // Step 3 - the cancellation message itself, a dataset like any other, and the exchange
        // set carrying it plus the fileless cancellation of the old dataset, built from the
        // retained entry. The chain is passed even though the certificate has not changed,
        // because that is what a producer must do once it has - the reused signature verifies
        // only with the certificate that made it.
        Warning relit = drogdenBuoyRelit(cancelled);
        Dataset cancellationDataset = buildDataset(relit, CANCELLATION_TIME.toLocalDate());
        validateDataset(cancellationDataset);
        S124ExchangeSetFactory.Cancellation cancellation = new S124ExchangeSetFactory.Cancellation(
                S124ExchangeSetFactory.discoveryMetadataFromXml(Files.readString(retainedEntry)),
                CANCELLATION_TIME.toLocalDate(),
                originalSet.signingCertificatePems());
        String cancellationSetName = relit.compactId() + "-cancels-" + cancelled.compactId();
        byte[] cancellationZip = buildExchangeSet(List.of(cancellationDataset), List.of(cancellation), signer,
                "S-124 exchange set with Danish navigational warning " + relit.compactId()
                        + ", cancelling " + cancelled.compactId()).bytes();
        validateCatalogue(cancellationZip, repoRoot);
        verifyFilelessCancellation(cancellationZip, originalEntry, fileNameOf(cancellationDataset),
                CANCELLATION_TIME.toLocalDate());
        Path cancellingDir = Files.createDirectories(cancellationDir.resolve("3-cancellation"));
        Files.write(cancellingDir.resolve(cancellationSetName + ".zip"), cancellationZip);
        extractZip(cancellationZip, cancellingDir.resolve(cancellationSetName));
        writeCancellationReadme(cancellationDir, cancelled, relit, cancellationSetName);
        System.out.println("fileless cancellation OK  " + cancellationDir.getFileName() + "/ ("
                + relit.compactId() + " cancels " + cancelled.compactId() + ")");

        writeReadme(out, warnings, signer);
        System.out.println("All examples written to " + out.toAbsolutePath().normalize());
    }

    // ------------------------------------------------------------------
    // The six warnings
    // ------------------------------------------------------------------

    /** NW 011/26 - The Sound, Drogden Channel: light buoy unlit (point). */
    private Warning drogdenBuoyUnlit() {
        String id = "DK-NW-011-26";
        NavwarnPreamble pre = preamble(id, 11, WarningTypeLabel.COASTAL_NAVIGATIONAL_WARNING, 2,
                NavwarnTypeGeneralLabel.AIDS_TO_NAVIGATION_CHANGES, true,
                OffsetDateTime.of(2026, 8, 20, 6, 45, 0, 0, ZoneOffset.UTC),
                area("The Sound", "Sundet"), locality("Drogden Channel", "Drogden"),
                "The Sound. Drogden Channel. Light buoy unlit.",
                "Sundet. Drogden. Lystønde slukket.");
        pre.getAffectedChartPublications().add(chart("DK 134", LocalDate.of(2024, 3, 8)));

        NavwarnPart part = part(id, 1, pre,
                GF.createPoint(new Coordinate(12.7200, 55.5467)),
                "The Sound, Drogden Channel. The light buoy marking the W side of the channel in pos. "
                        + "55-32.8N 012-43.2E is unlit. Will be re-established as soon as possible.",
                "Sundet, Drogden. Lystønden på W-siden af løbet på pos. 55-32,8N 012-43,2E er slukket. "
                        + "Fyrbelysningen genetableres snarest muligt.");

        return warning(id, "The Sound. Drogden Channel. Light buoy unlit.", pre, part,
                QualityOfHorizontalMeasurementLabel.PRECISELY_KNOWN); // charted light buoy, position from the national AtoN register - the clause 8.11 example
    }

    /** NW 012/26 - Great Belt, East Channel: cable laying operations (curve, date range). */
    private Warning greatBeltCableWork() {
        String id = "DK-NW-012-26";
        NavwarnPreamble pre = preamble(id, 12, WarningTypeLabel.COASTAL_NAVIGATIONAL_WARNING, 2,
                NavwarnTypeGeneralLabel.WORKS, true,
                OffsetDateTime.of(2026, 8, 22, 10, 0, 0, 0, ZoneOffset.UTC),
                area("The Great Belt", "Storebælt"), locality("East Channel", "Østerrenden"),
                "The Great Belt. East Channel. Cable operations.",
                "Storebælt. Østerrenden. Kabelarbejde.");
        pre.setCancellationDate(OffsetDateTime.of(2026, 9, 14, 18, 0, 0, 0, ZoneOffset.UTC));

        NavwarnPart part = part(id, 1, pre,
                GF.createLineString(new Coordinate[] {
                        new Coordinate(11.0180, 55.3300),
                        new Coordinate(11.0560, 55.3050),
                        new Coordinate(11.0900, 55.2870)}),
                "The Great Belt, East Channel, S of the East Bridge. During 1-14 Sep 2026 cable laying "
                        + "operations will be carried out by C/V ISAAC NEWTON along a line joining "
                        + "55-19.8N 011-01.1E, 55-18.3N 011-03.4E and 55-17.2N 011-05.4E. Vessels are "
                        + "requested to pass at slow speed and give the operations a wide berth.",
                "Storebælt, Østerrenden, S for Østbroen. I perioden 1.-14. september 2026 udføres "
                        + "kabelarbejde af kabelskibet ISAAC NEWTON langs en linje mellem 55-19,8N 011-01,1E, "
                        + "55-18,3N 011-03,4E og 55-17,2N 011-05,4E. Skibsfarten anmodes om at passere med "
                        + "nedsat fart og holde godt klar af arbejdet.");
        part.getFixedDateRanges().add(dateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14), -1, -1));
        part.getFeatureNames().add(featureName("eng", "C/V ISAAC NEWTON"));

        return warning(id, "The Great Belt. East Channel. Cable operations.", pre, part,
                QualityOfHorizontalMeasurementLabel.APPROXIMATE); // a working cable vessel moves along the route
    }

    /** NW 013/26 - Kattegat, NW of Hatter Barn: drifting containers (point). */
    private Warning kattegatDriftingContainers() {
        String id = "DK-NW-013-26";
        NavwarnPreamble pre = preamble(id, 13, WarningTypeLabel.COASTAL_NAVIGATIONAL_WARNING, 2,
                NavwarnTypeGeneralLabel.DRIFTING_HAZARDS, true,
                OffsetDateTime.of(2026, 8, 25, 8, 40, 0, 0, ZoneOffset.UTC),
                area("Kattegat", "Kattegat"), locality("NW of Hatter Barn", "NV for Hatter Barn"),
                "Kattegat. NW of Hatter Barn. Containers adrift.",
                "Kattegat. NV for Hatter Barn. Drivende containere.");

        NavwarnPart part = part(id, 1, pre,
                GF.createPoint(new Coordinate(10.7667, 55.8833)),
                "Kattegat, NW of Hatter Barn. 3 containers reported adrift in pos. 55-53.0N 010-46.0E "
                        + "at 250800 UTC Aug. The containers are barely awash. Vessels are advised to keep "
                        + "a sharp lookout and pass with caution.",
                "Kattegat, NV for Hatter Barn. 3 containere er meldt drivende på pos. 55-53,0N 010-46,0E "
                        + "kl. 0800 UTC den 25. august. Containerne ligger næsten i vandoverfladen. "
                        + "Skibsfarten tilrådes at holde skarpt udkig og passere med forsigtighed.");

        return warning(id, "Kattegat. NW of Hatter Barn. Containers adrift.", pre, part,
                QualityOfHorizontalMeasurementLabel.APPROXIMATE); // drifting objects - the position does not remain fixed
    }

    /** NW 014/26 - Baltic Sea, S of Bornholm: firing exercises, entry prohibited (surface). */
    private Warning bornholmFiringExercise() {
        String id = "DK-NW-014-26";
        NavwarnPreamble pre = preamble(id, 14, WarningTypeLabel.COASTAL_NAVIGATIONAL_WARNING, 2,
                NavwarnTypeGeneralLabel.SPECIAL_OPERATIONS, true,
                OffsetDateTime.of(2026, 8, 24, 12, 0, 0, 0, ZoneOffset.UTC),
                area("The Baltic Sea", "Østersøen"), locality("S of Bornholm", "S for Bornholm"),
                "Baltic Sea. S of Bornholm. Firing exercises.",
                "Østersøen. S for Bornholm. Skydeøvelser.");
        pre.setCancellationDate(OffsetDateTime.of(2026, 9, 11, 16, 0, 0, 0, ZoneOffset.UTC));

        NavwarnPart part = part(id, 1, pre,
                GF.createPolygon(new Coordinate[] {
                        new Coordinate(14.8500, 54.9950),
                        new Coordinate(15.0500, 54.9950),
                        new Coordinate(15.0500, 54.8700),
                        new Coordinate(14.8500, 54.8700),
                        new Coordinate(14.8500, 54.9950)}),
                "Baltic Sea, S of Bornholm. Firing practice area EK D 371 Raghammer. Gunnery exercises "
                        + "will take place 7-11 Sep 2026 between 0600 and 1400 UTC in the area bounded by "
                        + "54-59.7N 014-51.0E, 54-59.7N 015-03.0E, 54-52.2N 015-03.0E and 54-52.2N 014-51.0E. "
                        + "Entry prohibited during firing. Further information on VHF channel 16.",
                "Østersøen, S for Bornholm. Skydeområde EK D 371 Raghammer. Skydeøvelser gennemføres "
                        + "7.-11. september 2026 mellem kl. 0600 og 1400 UTC i området afgrænset af 54-59,7N 014-51,0E, "
                        + "54-59,7N 015-03,0E, 54-52,2N 015-03,0E og 54-52,2N 014-51,0E. Sejlads i området er "
                        + "forbudt, når skydning pågår. Yderligere oplysninger på VHF kanal 16.");
        RestrictionType restriction = OF.createRestrictionType();
        restriction.setValue(RestrictionLabel.ENTRY_PROHIBITED);
        restriction.setCode(BigInteger.valueOf(7)); // S-57 RESTRN 7 = entry prohibited
        part.setRestriction(restriction);
        part.getFixedDateRanges().add(dateRange(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 11), 6, 14));

        return warning(id, "Baltic Sea. S of Bornholm. Firing exercises.", pre, part,
                QualityOfHorizontalMeasurementLabel.PRECISELY_KNOWN); // EK D 371 is a charted area with defined corners
    }

    /** NW 015/26 - North Sea, Horns Rev: wind turbine unlit (point). */
    private Warning hornsRevTurbineUnlit() {
        String id = "DK-NW-015-26";
        NavwarnPreamble pre = preamble(id, 15, WarningTypeLabel.COASTAL_NAVIGATIONAL_WARNING, 2,
                NavwarnTypeGeneralLabel.OFFSHORE_INFRASTRUCTURE, true,
                OffsetDateTime.of(2026, 8, 23, 15, 20, 0, 0, ZoneOffset.UTC),
                area("The North Sea", "Nordsøen"), locality("Horns Rev", "Horns Rev"),
                "North Sea. Horns Rev. Wind turbine unlit.",
                "Nordsøen. Horns Rev. Vindmølle slukket.");

        NavwarnPart part = part(id, 1, pre,
                GF.createPoint(new Coordinate(7.7000, 55.7000)),
                "North Sea, Horns Rev. Wind turbine D-04 in Horns Rev 3 Offshore Wind Farm, pos. "
                        + "55-42.0N 007-42.0E, is unlit. Vessels are requested to keep well clear.",
                "Nordsøen, Horns Rev. Vindmølle D-04 i Horns Rev 3 Havmøllepark, pos. 55-42,0N 007-42,0E, "
                        + "er slukket. Skibsfarten anmodes om at holde godt klar.");
        part.getFeatureNames().add(featureName("eng", "WTG D-04"));

        return warning(id, "North Sea. Horns Rev. Wind turbine unlit.", pre, part,
                QualityOfHorizontalMeasurementLabel.PRECISELY_KNOWN); // fixed offshore structure with a surveyed position
    }

    /** NW 016/26 - Limfjorden, Aalborg: railway bridge closed; cancels NW 009/26 (point + References). */
    private Warning limfjordenBridgeClosed() {
        String id = "DK-NW-016-26";
        NavwarnPreamble pre = preamble(id, 16, WarningTypeLabel.LOCAL_NAVIGATIONAL_WARNING, 1,
                NavwarnTypeGeneralLabel.OTHER_HAZARDS, false,
                OffsetDateTime.of(2026, 8, 25, 9, 30, 0, 0, ZoneOffset.UTC),
                area("Limfjorden", "Limfjorden"), locality("Aalborg", "Aalborg"),
                "Limfjorden. Aalborg. Railway bridge closed for passage.",
                "Limfjorden. Aalborg. Jernbanebroen lukket for gennemsejling.");

        NavwarnPart part = part(id, 1, pre,
                GF.createPoint(new Coordinate(9.9040, 57.0570)),
                "Limfjorden, Aalborg. The railway bridge across Limfjorden is unable to open due to a "
                        + "technical failure. Passage is closed for vessels requiring bridge opening until "
                        + "further notice.",
                "Limfjorden, Aalborg. Jernbanebroen over Limfjorden kan ikke åbnes på grund af en teknisk "
                        + "fejl. Gennemsejling er indtil videre indstillet for skibe, der kræver broåbning.");

        // A plain warning. It used to cancel an earlier warning 009/26 with a References of
        // referenceCategory 1 - but S-124 clause 9.3 pairs such a References with a fileless
        // cancellation of the cancelled dataset in the same exchange set, and 009/26 has no
        // dataset in these examples. Cancelling is demonstrated by NW 018/26 instead, in full.
        return warning(id, "Limfjorden. Aalborg. Railway bridge closed for passage.", pre, part,
                QualityOfHorizontalMeasurementLabel.PRECISELY_KNOWN); // fixed bridge structure
    }

    // ------------------------------------------------------------------
    // The cancellation
    // ------------------------------------------------------------------

    /**
     * NW 018/26 - cancels NW 011/26: the Drogden light buoy is back in service.
     *
     * <p>S-124 clause 9.3 (second bullet) and Table 8-1 "New dataset with cancellation": one
     * NavwarnPreamble - a cancellation is itself a numbered message in the series - and one
     * References with noMessageOnHand false, referenceCategory 1 (warning cancellation) and the
     * messageSeriesIdentifier of the warning being cancelled. Clause 9.3 has this dataset carry
     * "only one instance of a References", so there is no NavwarnPart and no geometry: the
     * dataset conveys nothing but the cancellation. Its discovery metadata still needs an
     * extent, and the natural one is the cancelled warning's.</p>
     */
    private Warning drogdenBuoyRelit(Warning cancelled) {
        String id = "DK-NW-018-26";
        String titleEn = "The Sound. Drogden Channel. Light buoy re-established. Nav. warn. 011/26 cancelled.";
        NavwarnPreamble pre = preamble(id, CANCELLATION_NUMBER, WarningTypeLabel.COASTAL_NAVIGATIONAL_WARNING, 2,
                NavwarnTypeGeneralLabel.AIDS_TO_NAVIGATION_CHANGES, true, CANCELLATION_TIME,
                area("The Sound", "Sundet"), locality("Drogden Channel", "Drogden"),
                titleEn,
                "Sundet. Drogden. Lystønde genetableret. Navigationsadvarsel 011/26 annulleres.");
        pre.getAffectedChartPublications().add(chart("DK 134", LocalDate.of(2024, 3, 8)));

        References refs = OF.createReferences();
        refs.setId(id + ".REF.1");
        refs.setNoMessageOnHand(false);
        var category = OF.createReferenceCategoryType();
        category.setValue(ReferenceCategoryLabel.WARNING_CANCELLATION);
        category.setCode(BigInteger.ONE);
        refs.setReferenceCategory(category);
        // The cancelled warning's own messageSeriesIdentifier, so number, type and
        // interoperabilityIdentifier are exactly those of the message being withdrawn.
        refs.getMessageSeriesIdentifiers().add(messageSeriesIdentifierOf(cancelled));
        refs.setTheWarning(href(pre.getId(), "theWarning"));
        pre.getTheReferences().add(href(refs.getId(), "theReferences"));

        return new Warning(id, titleEn,
                "Danish navigational warning " + id + " issued by the " + AGENCY + ", cancelling "
                        + cancelled.compactId() + ". " + titleEn,
                new ArrayList<>(List.of(pre, refs)), cancelled.envelope());
    }

    // ------------------------------------------------------------------
    // S-124 model helpers
    // ------------------------------------------------------------------

    private Warning warning(String id, String titleEn, NavwarnPreamble pre, NavwarnPart part,
            QualityOfHorizontalMeasurementLabel quality) {
        Envelope env = new Envelope();
        for (NavwarnPart.Geometry g : part.getGeometries()) {
            List<S100SpatialAttributeType> attrs = new ArrayList<>();
            if (g.getPointProperty() != null) attrs.add(g.getPointProperty());
            if (g.getCurveProperty() != null) attrs.add(g.getCurveProperty());
            if (g.getSurfaceProperty() != null) attrs.add(g.getSurfaceProperty());
            Geometry jts = GeometryS124Converter.pointCurveSurfaceToGeometry(attrs);
            env.expandToInclude(jts.getEnvelopeInternal());
        }
        env.expandBy(0.05);
        // S-124 clause 8.11: "Geometry in datasets should by default have a qualityOfPosition set
        // to 4 (approximate). Other values should only be chosen when source material justify such
        // values." The GML schema spells the attribute qualityOfHorizontalMeasurement and carries
        // it on a SpatialQuality information type.
        SpatialQuality sq = OF.createSpatialQuality();
        sq.setId(id + ".SQ.1");
        QualityOfHorizontalMeasurementType q = OF.createQualityOfHorizontalMeasurementType();
        q.setValue(quality);
        sq.setQualityOfHorizontalMeasurement(q);

        List<AbstractGMLType> members = new ArrayList<>(List.of(pre, part, sq));
        return new Warning(id, titleEn,
                "Danish navigational warning " + id + " issued by the " + AGENCY + ". " + titleEn,
                members, env);
    }

    private NavwarnPreamble preamble(String id, int number, WarningTypeLabel warningType, int warningTypeCode,
            NavwarnTypeGeneralLabel typeGeneral, boolean intService, OffsetDateTime published,
            GeneralAreaType area, LocalityType locality, String titleEn, String titleDa) {
        NavwarnPreamble pre = OF.createNavwarnPreamble();
        pre.setId(id + ".PRE.1");
        pre.setMessageSeriesIdentifier(msi(number, warningType, warningTypeCode));
        pre.getGeneralAreas().add(area);
        pre.getLocalities().add(locality);
        pre.getNavwarnTitles().add(title("eng", titleEn));
        pre.getNavwarnTitles().add(title("dan", titleDa));
        NavwarnTypeGeneralType general = OF.createNavwarnTypeGeneralType();
        general.setValue(typeGeneral);
        pre.setNavwarnTypeGeneral(general);
        pre.setIntService(intService);
        pre.setPublicationTime(published);
        return pre;
    }

    /**
     * S-124 clause 8.1.2, Table 8-1 "In-force bulletin": one NavwarnPreamble, one References with
     * referenceCategory 3 (in-force), and - since noMessageOnHand is false - one
     * messageSeriesIdentifier per warning still in force. No geometry, no NavwarnAreaAffected,
     * no TextPlacement. Clause 8.1.3: the bulletin should reference itself as active.
     */
    private Dataset buildInForceBulletin(List<Warning> warnings, Envelope envelope) {
        String id = "DK-NW-2026-in-force";
        NavwarnPreamble pre = preamble(id, BULLETIN_NUMBER,
                WarningTypeLabel.COASTAL_NAVIGATIONAL_WARNING, 2,
                NavwarnTypeGeneralLabel.IN_FORCE_BULLETIN, true,
                OffsetDateTime.of(2026, 8, 25, 12, 0, 0, 0, ZoneOffset.UTC),
                area("Danish waters", "Danske farvande"),
                locality("All areas", "Alle omraader"),
                "Danish navigational warnings in force.",
                "Danske navigationsadvarsler i kraft.");

        References refs = OF.createReferences();
        refs.setId(id + ".REF.1");
        var category = OF.createReferenceCategoryType();
        category.setValue(ReferenceCategoryLabel.IN_FORCE);
        category.setCode(BigInteger.valueOf(3));
        refs.setReferenceCategory(category);
        refs.setNoMessageOnHand(false);
        refs.setTheWarning(href(pre.getId(), "theWarning"));
        // Every warning still in force, then the bulletin itself (clause 8.1.3). Each reference
        // reuses the referenced warning's own messageSeriesIdentifier, so its warningType,
        // number and interoperabilityIdentifier cannot drift from the warning it denotes -
        // warning 016 is a Local warning, not a Coastal one.
        for (Warning w : warnings) {
            refs.getMessageSeriesIdentifiers().add(messageSeriesIdentifierOf(w));
        }
        refs.getMessageSeriesIdentifiers().add(pre.getMessageSeriesIdentifier());
        pre.getTheReferences().add(href(refs.getId(), "theReferences"));

        return buildDataset(id, "Danish navigational warnings in force",
                "In-force bulletin listing all Danish navigational warnings in force, issued by the "
                        + AGENCY + ".",
                envelope, List.of(pre, refs), DATASET_REFERENCE_DATE);
    }

    /** The messageSeriesIdentifier a warning declares in its own NavwarnPreamble. */
    private MessageSeriesIdentifierType messageSeriesIdentifierOf(Warning w) {
        return w.members().stream()
                .filter(NavwarnPreamble.class::isInstance)
                .map(NavwarnPreamble.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("warning " + w.compactId() + " has no preamble"))
                .getMessageSeriesIdentifier();
    }

    /**
     * Remove previously generated datasets/exchange sets so stale names cannot survive a rename.
     * Extracted copies are directories, so those go too.
     */
    private void deleteGeneratedFiles(Path dir) throws IOException {
        try (var entries = Files.list(dir)) {
            for (Path f : entries.toList()) {
                String n = f.getFileName().toString();
                if (Files.isDirectory(f)) {
                    deleteTree(f);
                } else if (n.endsWith(".GML") || n.endsWith(".xml") || n.endsWith(".zip")) {
                    Files.delete(f);
                }
            }
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            for (Path p : walk.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    /** The dataset's own declared file name (S-100 Part 10b, Table 10b-4). */
    private String fileNameOf(Dataset ds) {
        return ds.getDatasetIdentificationInformation().getDatasetFileIdentifier();
    }

    /**
     * Marshals the dataset, validates it against the S-124 XSD and writes it under its own
     * file name - Part 10b Table 10b-4 defines datasetFileIdentifier as the file name, so the
     * standalone copy carries the same name as the packaged one.
     */
    private void writeDataset(Path dir, Dataset ds) throws Exception {
        Files.writeString(dir.resolve(fileNameOf(ds)), validateDataset(ds));
    }

    /** Marshals the dataset and validates the XML against the S-124 XSD. */
    private String validateDataset(Dataset ds) throws Exception {
        String xml = S124Utils.marshalS124(ds);
        S124XsdValidator.validate(xml);
        return xml;
    }

    private NavwarnPart part(String id, int n, NavwarnPreamble pre, Geometry jtsGeometry,
            String textEn, String textDa) {
        NavwarnPart part = OF.createNavwarnPart();
        part.setId(id + ".NP." + n);
        part.setHeader(href(pre.getId(), "header"));

        WarningInformationType info = OF.createWarningInformationType();
        info.getInformations().add(information("eng", textEn));
        info.getInformations().add(information("dan", textDa));
        part.setWarningInformation(info);

        int geomIdx = 1;
        for (S100SpatialAttributeType attr : GeometryS124Converter.geometryToS124PointCurveSurfaceGeometry(jtsGeometry)) {
            NavwarnPart.Geometry geometry = OF.createNavwarnPartGeometry();
            String geomId = id + ".G." + n + "." + geomIdx++;
            if (attr instanceof PointProperty p) {
                p.getPoint().setId(geomId);
                p.getPoint().setSrsName("EPSG:4326");
                geometry.setPointProperty(p);
            } else if (attr instanceof CurveProperty c) {
                c.getCurve().setId(geomId);
                c.getCurve().setSrsName("EPSG:4326");
                geometry.setCurveProperty(c);
            } else if (attr instanceof SurfaceProperty s) {
                s.getSurface().setId(geomId);
                s.getSurface().setSrsName("EPSG:4326");
                geometry.setSurfaceProperty(s);
            } else {
                throw new IllegalStateException("Unexpected spatial attribute " + attr);
            }
            part.getGeometries().add(geometry);
        }
        return part;
    }

    private MessageSeriesIdentifierType msi(int number, WarningTypeLabel warningType, int warningTypeCode) {
        MessageSeriesIdentifierType m = OF.createMessageSeriesIdentifierType();
        m.setAgencyResponsibleForProduction(AGENCY_CODE);
        m.setNameOfSeries(SERIES);
        m.setNationality("DK");
        m.setWarningNumber(number);
        m.setYear(YEAR);
        var type = OF.createWarningTypeType();
        type.setValue(warningType);
        type.setCode(BigInteger.valueOf(warningTypeCode));
        m.setWarningType(type);
        // S-124 clause 12.2.2: datasetID "must be an MRN and if used match the value of
        // interoperabilityIdentifier in the messageSeriesIdentifier". Supplying it here makes the
        // message self-identifying; the library then derives datasetID from it rather than
        // synthesising one.
        m.setInteroperabilityIdentifier(String.format("urn:mrn:iho:s124:dk:%d:%d", YEAR, number));
        return m;
    }

    private GeneralAreaType area(String en, String da) {
        GeneralAreaType area = OF.createGeneralAreaType();
        area.getLocationNames().add(locationName("eng", en));
        area.getLocationNames().add(locationName("dan", da));
        return area;
    }

    private LocalityType locality(String en, String da) {
        LocalityType locality = OF.createLocalityType();
        locality.getLocationNames().add(locationName("eng", en));
        locality.getLocationNames().add(locationName("dan", da));
        return locality;
    }

    private LocationNameType locationName(String lang, String text) {
        LocationNameType n = OF.createLocationNameType();
        n.setLanguage(lang);
        n.setText(text);
        return n;
    }

    private NavwarnTitleType title(String lang, String text) {
        NavwarnTitleType t = OF.createNavwarnTitleType();
        t.setLanguage(lang);
        t.setText(text);
        return t;
    }

    private InformationType information(String lang, String text) {
        InformationType i = OF.createInformationType();
        i.setLanguage(lang);
        i.setText(text);
        return i;
    }

    private FeatureNameType featureName(String lang, String name) {
        FeatureNameType f = OF.createFeatureNameType();
        f.setLanguage(lang);
        f.setName(name);
        return f;
    }

    private AffectedChartPublicationsType chart(String chartNumber, LocalDate editionDate) {
        var chartAffected = OF.createChartAffectedType();
        chartAffected.setChartNumber(chartNumber);
        chartAffected.setEditionDate(editionDate);
        AffectedChartPublicationsType pub = OF.createAffectedChartPublicationsType();
        pub.setLanguage("eng");
        pub.setChartAffected(chartAffected);
        return pub;
    }

    /** Date range; pass hourStart/hourEnd &lt; 0 to omit the time of day. */
    private FixedDateRangeType dateRange(LocalDate start, LocalDate end, int hourStart, int hourEnd) {
        FixedDateRangeType range = OF.createFixedDateRangeType();
        var s = OF.createDateStartType();
        s.setDate(start);
        range.setDateStart(s);
        var e = OF.createDateEndType();
        e.setDate(end);
        range.setDateEnd(e);
        if (hourStart >= 0 && hourEnd >= 0) {
            try {
                DatatypeFactory dtf = DatatypeFactory.newInstance();
                range.setTimeOfDayStart(dtf.newXMLGregorianCalendarTime(hourStart, 0, 0, 0));
                range.setTimeOfDayEnd(dtf.newXMLGregorianCalendarTime(hourEnd, 0, 0, 0));
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
        return range;
    }

    private ReferenceType href(String gmlId, String role) {
        ReferenceType ref = new ReferenceTypeImpl();
        ref.setHref("#" + gmlId);
        // S-100 Part 10b clause 10b-9: an association must carry role and/or arcrole so a reader
        // can tell it from an attribute. S-124 defines no values, so the producer picks them.
        ref.setRole(role);
        return ref;
    }

    private Dataset buildDataset(Warning w, LocalDate referenceDate) {
        return buildDataset(w.compactId(), w.titleEn(), w.abstractEn(), w.envelope(), w.members(), referenceDate);
    }

    private Dataset buildDataset(String compactId, String title, String abstractText,
            Envelope envelope, List<AbstractGMLType> members, LocalDate referenceDate) {
        Dataset dataset = OF.createDataset();
        dataset.setId(compactId);

        DataSetIdentificationTypeImpl ident = new DataSetIdentificationTypeImpl();
        ident.setEncodingSpecification("S-100 Part 10b");
        ident.setEncodingSpecificationEdition("1.0");
        ident.setProductIdentifier("S-124");
        ident.setProductEdition("2.0.0");
        // S-100 Part 10b Table 10b-4: "1" = base dataset, "2" = update
        ident.setApplicationProfile("1");
        // S-100 Part 10b Table 10b-4: the identifier is the packaged file name, which must follow
        // the Part 17 clause 17-4.3 pattern 124<producer><alphanumeric>.GML
        ident.setDatasetFileIdentifier("124DK00" + compactId.replaceAll("[^A-Za-z0-9]", "") + ".GML");
        ident.setDatasetTitle(title);
        ident.setDatasetReferenceDate(referenceDate);
        ident.setDatasetLanguage("eng");
        ident.setDatasetAbstract(abstractText);
        ident.getDatasetTopicCategories().add(
                dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.MDTopicCategoryCode.OCEANS);
        ident.setDatasetPurpose(
                dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.DatasetPurposeType.BASE);
        ident.setUpdateNumber(BigInteger.ZERO);
        dataset.setDatasetIdentificationInformation(ident);

        // GML positions are lat,lon; the JTS envelope is lon,lat.
        PosImpl lower = new PosImpl();
        lower.setValue(new Double[] { envelope.getMinY(), envelope.getMinX() });
        PosImpl upper = new PosImpl();
        upper.setValue(new Double[] { envelope.getMaxY(), envelope.getMaxX() });
        EnvelopeTypeImpl env = new EnvelopeTypeImpl();
        env.setSrsName("EPSG:4326");
        env.setLowerCorner(lower);
        env.setUpperCorner(upper);
        BoundingShapeTypeImpl bbox = new BoundingShapeTypeImpl();
        bbox.setEnvelope(env);
        dataset.setBoundedBy(bbox);

        Dataset.Members membersElement = OF.createDatasetMembers();
        membersElement.getNavwarnPartsAndNavwarnAreaAffectedsAndTextPlacements().addAll(members);
        dataset.setMembers(membersElement);
        return dataset;
    }

    // ------------------------------------------------------------------
    // Exchange sets and signing
    // ------------------------------------------------------------------

    private S124ExchangeSetFactory.ExchangeSet buildExchangeSet(List<Dataset> datasets,
            List<S124ExchangeSetFactory.Cancellation> cancellations, SignerBundle signer, String description) {
        return S124ExchangeSetFactory.builder()
                .datasets(datasets)
                .cancellations(cancellations)
                .organization(AGENCY)
                .producerCode("DK00")
                .certificatePem(signer.certificatePem())
                .signer(signer.signer())
                .signatureAlgorithm(signer.algorithm())
                .emails(List.of("nautinf@dma.dk"))
                .phone("+4572196000")
                .city("Korsoer")
                .postalCode("4220")
                .country("Denmark")
                .description(description)
                .build()
                .toExchangeSet();
    }

    private record SignerBundle(String certificatePem, S124Signer signer,
                                S100SEDigitalSignatureReference algorithm, String note) {}

    /**
     * Creates an EC P-384 key and self-signed certificate in the JVM and returns a signer
     * producing real ECDSA signatures in the form S-100 Part 15, clause 15-8.4, embeds - the DER
     * SEQUENCE of r and s, which "SHA384withECDSA" yields. The key and certificate are written to
     * {@code signingDir} in PEM form so the generated bundle documents what signed it.
     */
    private SignerBundle createSigner(Path signingDir) throws Exception {
        SigningIdentityFixture identity = SigningIdentityFixture.selfSigned("S-124 Example Signer");
        Files.writeString(signingDir.resolve("example-signer-cert.pem"),
                pem("CERTIFICATE", identity.certificate().getEncoded()));
        Files.writeString(signingDir.resolve("example-signer-key-pkcs8.pem"),
                pem("PRIVATE KEY", identity.privateKey().getEncoded()));
        return new SignerBundle(identity.certificatePem(), identity.signer(),
                S100SEDigitalSignatureReference.ECDSA_384_SHA_2,
                "Real ECDSA " + SigningIdentityFixture.JCA_ALGORITHM + " signatures - the DER SEQUENCE of r and s "
                        + "that S-100 Part 15, clause 15-8.4, embeds - made with a throwaway self-signed EC "
                        + "P-384 certificate generated for this run (see signing/).");
    }

    private static String pem(String label, byte[] der) {
        return "-----BEGIN " + label + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der)
                + "\n-----END " + label + "-----\n";
    }

    // ------------------------------------------------------------------
    // Validation and output
    // ------------------------------------------------------------------

    /** Validates the CATALOG.XML inside the exchange set zip against the S-100 5.2.0 catalogue XSD. */
    private void validateCatalogue(byte[] zip, Path repoRoot) throws Exception {
        String catalogXml = null;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("S100_ROOT/CATALOG.XML")) {
                    catalogXml = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        if (catalogXml == null) {
            throw new AssertionError("exchange set has no S100_ROOT/CATALOG.XML");
        }
        List<String> errors = new ArrayList<>();
        Validator validator = catalogueSchema(repoRoot).newValidator();
        validator.setErrorHandler(new ErrorHandler() {
            @Override public void warning(SAXParseException e) {}
            @Override public void error(SAXParseException e) { errors.add("line " + e.getLineNumber() + ": " + e.getMessage()); }
            @Override public void fatalError(SAXParseException e) { errors.add("fatal, line " + e.getLineNumber() + ": " + e.getMessage()); }
        });
        validator.validate(new StreamSource(new StringReader(catalogXml)));
        if (!errors.isEmpty()) {
            throw new AssertionError("CATALOG.XML is not schema valid:\n" + String.join("\n", errors));
        }
    }

    /**
     * Checks that the cancellation exchange set encodes what S-100 Part 17, clause 17-4.4.1,
     * describes - the example illustrates that text, so it must not be allowed to drift from it:
     * no file is shipped for the cancelled dataset, and its entry is the published one with the
     * purpose and issue date changed and the original signature kept.
     */
    private static void verifyFilelessCancellation(byte[] zip, S124ExchangeSetFactory.PublishedDataset original,
            String cancellationDatasetFileName, LocalDate cancellationDate) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.put(entry.getName(), zis.readAllBytes());
            }
        }
        String datasetFiles = "S100_ROOT/S-124/DATASET_FILES/";
        assertThat(entries.keySet().stream().filter(n -> n.startsWith(datasetFiles) && !n.endsWith("/")).toList())
                .as("a fileless cancellation ships no file for the cancelled dataset")
                .containsExactly(datasetFiles + cancellationDatasetFileName);

        S100ExchangeCatalogue catalogue = S100ExchangeSetUtils.unmarshallS100ExchangeSetCatalogue(
                new String(entries.get("S100_ROOT/CATALOG.XML"), StandardCharsets.UTF_8));
        List<S100DatasetDiscoveryMetadata> metadata =
                catalogue.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas();
        assertThat(metadata).hasSize(2);
        assertThat(metadata.get(0).getPurpose()).isEqualTo(S100Purpose.NEW_DATASET);
        assertThat(metadata.get(0).getFileName()).endsWith("/" + cancellationDatasetFileName);

        S100DatasetDiscoveryMetadata cancellation = metadata.get(1);
        S100DatasetDiscoveryMetadata published = original.discoveryMetadata();
        assertThat(cancellation.getPurpose()).isEqualTo(S100Purpose.CANCELLATION);
        assertThat(cancellation.getIssueDate()).isEqualTo(cancellationDate);
        assertThat(cancellation.getFileName()).isEqualTo(published.getFileName())
                .endsWith("/" + original.fileName());
        assertThat(cancellation.getDatasetID()).isEqualTo(published.getDatasetID());
        assertThat(signatureOf(cancellation)).isEqualTo(signatureOf(published));
    }

    private static byte[] signatureOf(S100DatasetDiscoveryMetadata entry) {
        return entry.getDigitalSignatureValues().get(0).getS100SEDigitalSignature().getValue().getValue();
    }

    private static Schema catalogueSchemaCache;

    private static synchronized Schema catalogueSchema(Path repoRoot) throws Exception {
        if (catalogueSchemaCache == null) {
            Path schemaPath = repoRoot.resolve("s-100/src/main/resources/xsd/S100Catalog/20240415/S100_ExchangeCatalogue.xsd");
            catalogueSchemaCache = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                    .newSchema(schemaPath.toFile());
        }
        return catalogueSchemaCache;
    }

    private static void extractZip(byte[] zip, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path target = targetDir.resolve(entry.getName()).normalize();
                if (!target.startsWith(targetDir)) {
                    throw new IOException("zip entry escapes target dir: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.write(target, zis.readAllBytes());
                }
            }
        }
    }

    private void writeReadme(Path out, List<Warning> warnings, SignerBundle signer) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Example S-124 navigational warnings - Danish waters\n\n");
        sb.append("Generated examples (NOT for navigation, NOT to be checked in). ");
        sb.append("Six fictitious but realistically formed Danish navigational warnings as S-124 v2.0.0 GML\n");
        sb.append("datasets, plus S-100 Ed 5.2.0 Part 17 exchange sets containing the same warnings.\n\n");
        sb.append("## Warnings\n\n");
        sb.append("| Id | Area | Content |\n|---|---|---|\n");
        for (Warning w : warnings) {
            sb.append("| ").append(w.compactId()).append(" | ")
              .append(w.titleEn().substring(0, w.titleEn().indexOf('.'))).append(" | ")
              .append(w.titleEn()).append(" |\n");
        }
        sb.append("\nAll warnings carry bilingual (eng/dan) titles and warning texts. Geometries cover the\n");
        sb.append("three S-124 spatial shapes: points (buoy, container, turbine, bridge), a curve (cable\n");
        sb.append("route in the Great Belt) and a surface (firing practice area EK D 371 south of Bornholm).\n\n");
        sb.append("## Layout\n\n");
        sb.append("- `datasets/` - one S-124 GML dataset per warning plus `DK-NW-2026-in-force.xml`,\n");
        sb.append("  an In-Force Bulletin (S-124 clause 8.1.2, Table 8-1): a single NavwarnPreamble\n");
        sb.append("  with one References of referenceCategory 3 (in-force) listing the six warnings\n");
        sb.append("  still in force plus itself, and carrying no geometry. Files are named by their own\n");
        sb.append("  datasetFileIdentifier, so a standalone copy and its packaged ZIP entry share one name.\n");
        sb.append("  All files validate against the\n");
        sb.append("  S-124 2.0.0 XSD.\n");
        sb.append("- `exchange-sets/` - one exchange set (zip) per warning, one for the bulletin, plus\n");
        sb.append("  `DK-NW-2026-all.zip` containing all seven datasets. `DK-NW-2026-all/` is an extracted copy for easy\n");
        sb.append("  inspection. Every CATALOG.XML validates against the S-100 5.2.0 exchange\n");
        sb.append("  catalogue XSD.\n");
        sb.append("- `fileless-cancellation/` - a self-contained worked example of cancelling DK-NW-011-26:\n");
        sb.append("  a cancellation dataset delivered together with a fileless cancellation of the original\n");
        sb.append("  (S-124 clause 9.3, S-100 Part 17 clause 17-4.4.1). It has its own README and can be handed\n");
        sb.append("  over as it is; nothing in it depends on the rest of this directory.\n");
        sb.append("- `signing/` - the throwaway key/certificate used for the signatures.\n\n");
        sb.append("## Signatures\n\n").append(signer.note()).append("\n\n");
        sb.append("Regenerate with:\n\n");
        sb.append("```\nmvn -pl s-124 -am test -Dtest=DanishWatersExamplesGenerator -Dsurefire.failIfNoSpecifiedTests=false\n```\n");
        sb.append("(generator source: `s-124/src/test/java/dk/dma/niord/s100/xmlbindings/s124/v2_0_0/examples/DanishWatersExamplesGenerator.java`)\n");
        Files.writeString(out.resolve("README.md"), sb.toString());
    }

    /**
     * The README of the hand-over folder. It is written for a reader who has only that folder -
     * the S-124 Project Team, for its Data Classification and Encoding Guide - so it repeats the
     * scenario and the specification text and refers to nothing outside the folder.
     */
    private void writeCancellationReadme(Path dir, Warning cancelled, Warning cancelling,
            String cancellationSetName) throws IOException {
        String cancelledFile = "124DK00" + cancelled.compactId().replaceAll("[^A-Za-z0-9]", "") + ".GML";
        String cancellingFile = "124DK00" + cancelling.compactId().replaceAll("[^A-Za-z0-9]", "") + ".GML";
        String cancellationDate = CANCELLATION_TIME.toLocalDate().toString();
        String extractedOriginal = "1-original/" + cancelled.compactId();
        String extractedCancelling = "3-cancellation/" + cancellationSetName;
        StringBuilder sb = new StringBuilder();
        sb.append("# S-124 fileless cancellation - worked example\n\n");
        sb.append("How a published S-124 navigational warning is cancelled in practice: a cancellation dataset\n");
        sb.append("delivered together with a fileless cancellation of the original dataset, in one S-100 exchange\n");
        sb.append("set. Produced by the Danish Maritime Authority with the Niord S-124 bindings\n");
        sb.append("(https://github.com/NiordOrg/niord-xml-bindings), S-124 Ed 2.0.0 / S-100 Ed 5.2.0.\n\n");
        sb.append("The warnings are fictitious and NOT for navigation; the exchange sets are marked\n");
        sb.append("`notForNavigation` and signed with a throwaway self-signed EC P-384 certificate made for this\n");
        sb.append("run. The signatures themselves are real (ECDSA SHA-384, DER-encoded r,s as S-100 Part 15,\n");
        sb.append("clause 15-8.4, requires), so they can be verified against the certificate in `CATALOG.XML`.\n\n");

        sb.append("## What S-124 and S-100 say\n\n");
        sb.append("S-124 clause 9.3 lists the ways an S-124 dataset is cancelled. The one a producer uses when a\n");
        sb.append("warning is withdrawn before any expiry date is the second: \"Sending a cancellation dataset\n");
        sb.append("which contains only one instance of a References information type with the referenceType\n");
        sb.append("attribute set to 1 (cancellation), and the messageReference with the identifier(s) of the\n");
        sb.append("dataset(s) to be cancelled, as well as including a fileless cancellation (see S-100 Part 17,\n");
        sb.append("clause 17-4.4.1) of the dataset being cancelled\". So two things travel in one exchange set:\n");
        sb.append("a new, numbered dataset that says what is cancelled, and a catalogue entry that withdraws the\n");
        sb.append("old dataset file.\n\n");
        sb.append("S-100 Part 17, clause 17-4.4.1, defines the latter: \"Fileless cancellation may be achieved by\n");
        sb.append("using a dataset metadata entry with the filename and original digital signature specifying\n");
        sb.append("the resource to be cancelled, and with all other mandatory metadata fields also set to the\n");
        sb.append("same values as the original, with the exception of the issueDate, which must be set to the\n");
        sb.append("issue date of the fileless cancellation itself.\"\n\n");

        sb.append("## Scenario\n\n");
        sb.append(cancelled.compactId()).append(" (").append(cancelled.titleEn())
          .append(") was published on 2026-08-20 as dataset `").append(cancelledFile).append("`.\n");
        sb.append("On ").append(cancellationDate).append(" the buoy is back in service and the warning is cancelled by ")
          .append(cancelling.compactId()).append("\n(").append(cancelling.titleEn()).append(").\n\n");

        sb.append("## Folder layout\n\n");
        sb.append("| Step | Folder | Contents |\n|---|---|---|\n");
        sb.append("| 1. Publish | `1-original/` | `").append(cancelled.compactId())
          .append(".zip`, the exchange set that published the warning, and an extracted copy. Its `CATALOG.XML` announces `")
          .append(cancelledFile).append("` with purpose `newDataset`; the file is in `S-124/DATASET_FILES/`. |\n");
        sb.append("| 2. Retain | `2-retained/` | `").append(cancelled.compactId())
          .append(".discovery-metadata.xml`, the `S100_DatasetDiscoveryMetadata` entry from step 1 as the producer keeps it,")
          .append(" together with the certificate that signed it. The cancellation must reproduce this entry, so it is")
          .append(" stored at publish time, not rebuilt later. |\n");
        sb.append("| 3. Cancel | `3-cancellation/` | `").append(cancellationSetName)
          .append(".zip`, the cancelling exchange set, and an extracted copy: two entries in `CATALOG.XML`, one dataset file")
          .append(" in `S-124/DATASET_FILES/`. |\n\n");

        sb.append("## What to look at in `").append(extractedCancelling).append("/S100_ROOT/CATALOG.XML`\n\n");
        sb.append("- The first `S100_DatasetDiscoveryMetadata`, purpose `newDataset`, announces `")
          .append(cancellingFile).append("`, the\n");
        sb.append("  cancellation dataset (S-124 Table 8-1 \"New dataset with cancellation\"; the file is in the\n");
        sb.append("  extracted copy under `S100_ROOT/S-124/DATASET_FILES/`): one NavwarnPreamble, message ")
          .append(String.format("%03d/%02d", CANCELLATION_NUMBER, YEAR % 100)).append(",\n");
        sb.append("  and one References with noMessageOnHand false, referenceCategory 1 (warning cancellation) and\n");
        sb.append("  the messageSeriesIdentifier of ").append(cancelled.compactId())
          .append(". No NavwarnPart, no geometry.\n");
        sb.append("- The second, purpose `cancellation`, is the fileless cancellation of `").append(cancelledFile)
          .append("`. It is\n");
        sb.append("  the entry from step 1 reproduced - same `fileName`, `datasetID`, bounding box, producing\n");
        sb.append("  agency and, above all, the same signature value - with two changes of content: `purpose` is\n");
        sb.append("  `cancellation` and `issueDate` is ").append(cancellationDate)
          .append(", the date of the cancellation. Diff it against\n");
        sb.append("  the entry in `").append(extractedOriginal).append("/S100_ROOT/CATALOG.XML` to see this. The\n");
        sb.append("  only other difference is the signature's `id` attribute, a catalogue-local label: the new\n");
        sb.append("  catalogue numbers its own dataset signatures from `sig1`, so the reused one is carried as\n");
        sb.append("  `sigC1` to keep ids unique. Its value and `certificateRef` are unchanged.\n");
        sb.append("- `S-124/DATASET_FILES/` holds no `").append(cancelledFile)
          .append("`: nothing is shipped for a cancelled dataset.\n");
        sb.append("- `CATALOG.SIGN` signs the whole catalogue with the producer's current certificate. The\n");
        sb.append("  signature reused in the cancellation entry is the one made over the original dataset file\n");
        sb.append("  when it was published, and still verifies against that file\n");
        sb.append("  (`").append(extractedOriginal).append("/S100_ROOT/S-124/DATASET_FILES/").append(cancelledFile)
          .append("`) with the certificate its\n");
        sb.append("  `certificateRef` points at. Had the producer changed certificate since publishing, the\n");
        sb.append("  certificate that made the original signature would be carried in `CATALOG.XML` alongside the\n");
        sb.append("  current one, so the entry can still be verified without external access (S-100 Part 15,\n");
        sb.append("  clause 15-8.7).\n\n");

        sb.append("## How the producer does it\n\n");
        sb.append("Keep the catalogue entry and the signing certificate chain of every published dataset (step 2).\n");
        sb.append("On cancellation, issue the cancellation message as a dataset and add the retained entry, with\n");
        sb.append("purpose `cancellation` and the cancellation's issue date, to the same exchange set (step 3).\n");
        sb.append("Both `CATALOG.XML` files here validate against the S-100 5.2.0 exchange catalogue XSD and both\n");
        sb.append("datasets against the S-124 2.0.0 XSD.\n");
        Files.writeString(dir.resolve("README.md"), sb.toString());
    }
}
