package com.aljodomo.kontis.persistence;

import com.aljodomo.kontis.model.Report;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Firebase Firestore client implementation via the Admin SDK.
 *
 * @author Aljoscha Domonell
 */
@Service
@Profile("!dev")
@Slf4j
public class FirebaseRepository implements ReportRepository {

    private final Firestore db;
    private final ObjectMapper mapper;

    @Autowired
    public FirebaseRepository(ObjectMapper objectMapper) throws IOException {

        log.info("Using Firestore as report repository");

        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        FirebaseApp.initializeApp(options);

        this.db = FirestoreClient.getFirestore();

        this.mapper = objectMapper;
    }

    @Override
    public void create(Report report) {
        db.runTransaction(transaction -> {
            var d = db.collection("reports")
                    .document(report.getId().toString())
                    .set(mapper.convertValue(report, new TypeReference<Map<String, Object>>() {
                    }));
            var res = d.get();
            log.info("Report created {}", res.getUpdateTime());
            return true;
        });
    }

    @SneakyThrows
    @Override
    public Report findById(String id) {
        return db.runTransaction(transaction -> {
            var d = db.collection("reports").document(id);
            var res = d.get();
            var data = res.get().getData();
            String json = mapper.writeValueAsString(data);
            return mapper.readValue(json, Report.class);
        }).get();
    }

    @Override
    public List<Report> findAll() {
        log.error("Not implemented method was called");
        return Collections.emptyList();
    }

    @Override
    public List<Report> findActive() {
        log.error("Not implemented method was called");
        return Collections.emptyList();
    }
}
