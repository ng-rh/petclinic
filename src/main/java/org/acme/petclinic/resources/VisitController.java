package org.acme.petclinic.resources;

import org.acme.petclinic.entities.Visit;
import org.acme.petclinic.repositories.VisitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/visits")
public class VisitController {

    @Autowired
    private VisitRepository visitRepository;

    @GetMapping
    public List<Visit> listAll() {
        return visitRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Visit> get(@PathVariable Long id) {
        Optional<Visit> visit = visitRepository.findById(id);
        return visit.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Visit create(@RequestBody Visit visit) {
        return visitRepository.save(visit);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Visit> update(@PathVariable Long id, @RequestBody Visit visitDetails) {
        Optional<Visit> visit = visitRepository.findById(id);
        if (visit.isPresent()) {
            Visit v = visit.get();
            v.setVisitDate(visitDetails.getVisitDate());
            v.setDescription(visitDetails.getDescription());
            v.setPet(visitDetails.getPet());
            v.setVet(visitDetails.getVet());
            return ResponseEntity.ok(visitRepository.save(v));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (visitRepository.existsById(id)) {
            visitRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
