package org.acme.petclinic.resources;

import org.acme.petclinic.entities.Pet;
import org.acme.petclinic.repositories.PetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/pets")
public class PetController {

    @Autowired
    private PetRepository petRepository;

    @GetMapping
    public List<Pet> listAll() {
        return petRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pet> get(@PathVariable Long id) {
        Optional<Pet> pet = petRepository.findById(id);
        return pet.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Pet create(@RequestBody Pet pet) {
        return petRepository.save(pet);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pet> update(@PathVariable Long id, @RequestBody Pet petDetails) {
        Optional<Pet> pet = petRepository.findById(id);
        if (pet.isPresent()) {
            Pet p = pet.get();
            p.setName(petDetails.getName());
            p.setBirthDate(petDetails.getBirthDate());
            p.setPetType(petDetails.getPetType());
            p.setOwner(petDetails.getOwner());
            return ResponseEntity.ok(petRepository.save(p));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (petRepository.existsById(id)) {
            petRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
