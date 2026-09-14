package example.cashcard.repositories;

import example.cashcard.models.CashCard;

import org.springframework.data.repository.CrudRepository;


public interface CashCardRepository extends CrudRepository<CashCard, Long> {}