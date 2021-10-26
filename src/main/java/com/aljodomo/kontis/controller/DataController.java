package com.aljodomo.kontis.controller;

import com.aljodomo.kontis.db.InMemoryDB;
import com.aljodomo.kontis.domain.Report;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Aljoscha Domonell
 */
@RestController
public class DataController {

    final InMemoryDB inMemoryDB;

    @Autowired
    public DataController(InMemoryDB inMemoryDB) {
        this.inMemoryDB = inMemoryDB;
    }

    @GetMapping("/activeReports")
    public ResponseEntity<List<Report>> activeReports() {
        return ResponseEntity.ok().body(this.inMemoryDB.getActiveReports());
    }

}
