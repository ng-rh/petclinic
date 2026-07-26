package org.acme.petclinic.resources;

import org.acme.petclinic.entities.Vet;
import org.acme.petclinic.repositories.VetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/vets")
public class VetController {

    @Autowired
    private VetRepository vetRepository;

    @GetMapping
    public List<Vet> listAll() {
        return vetRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vet> get(@PathVariable Long id) {
        Optional<Vet> vet = vetRepository.findById(id);
        return vet.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Vet create(@RequestBody Vet vet) {
        return vetRepository.save(vet);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vet> update(@PathVariable Long id, @RequestBody Vet vetDetails) {
        Optional<Vet> vet = vetRepository.findById(id);
        if (vet.isPresent()) {
            Vet v = vet.get();
            v.setFirstName(vetDetails.getFirstName());
            v.setLastName(vetDetails.getLastName());
            v.setSpecialty(vetDetails.getSpecialty());
            return ResponseEntity.ok(vetRepository.save(v));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (vetRepository.existsById(id)) {
            vetRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
