package com.example.demo.service;

import com.example.demo.model.BudgetCalculator;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class BudgetCalculatorServiceImpl implements BudgetCalculatorService {

    private static final String COLLECTION_NAME = "budgetCalculations";

    @Override
    public BudgetCalculator createBudgetCalculation(BudgetCalculator calculator) 
            throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference docRef = dbFirestore.collection(COLLECTION_NAME).document();
        
        // Set the ID and calculate total before saving
        calculator.setCalculatorId(docRef.getId());
        calculator.setTotalBudget(calculateTotal(calculator));
        
        ApiFuture<WriteResult> writeResult = docRef.set(calculator);
        writeResult.get(); // Wait for operation to complete
        
        return calculator;
    }

    @Override
    public BudgetCalculator getBudgetCalculationById(String calculatorId) 
            throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference docRef = dbFirestore.collection(COLLECTION_NAME).document(calculatorId);
        DocumentSnapshot document = docRef.get().get();

        if (document.exists()) {
            return document.toObject(BudgetCalculator.class);
        } else {
            throw new IllegalArgumentException("Budget calculation not found with ID: " + calculatorId);
        }
    }

    @Override
    public List<BudgetCalculator> getBudgetCalculationsByUser(String userId) 
            throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        Query query = dbFirestore.collection(COLLECTION_NAME)
                               .whereEqualTo("userId", userId)
                               .orderBy("calculatorId", Query.Direction.DESCENDING);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<BudgetCalculator> calculations = new ArrayList<>();

        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            calculations.add(document.toObject(BudgetCalculator.class));
        }

        return calculations;
    }

    @Override
    public BudgetCalculator updateBudgetCalculation(String calculatorId, BudgetCalculator calculator) 
            throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference docRef = dbFirestore.collection(COLLECTION_NAME).document(calculatorId);

        // Create update map and recalculate total
        Map<String, Object> updates = new HashMap<>();
        updates.put("destinationIds", calculator.getDestinationIds());
        updates.put("dailyAllowance", calculator.getDailyAllowance());
        updates.put("tripDurationDays", calculator.getTripDurationDays());
        updates.put("accommodationCost", calculator.getAccommodationCost());
        updates.put("transportCost", calculator.getTransportCost());
        updates.put("activityCosts", calculator.getActivityCosts());
        updates.put("notes", calculator.getNotes());
        updates.put("totalBudget", calculateTotal(calculator));

        ApiFuture<WriteResult> writeResult = docRef.update(updates);
        writeResult.get();

        // Return updated document
        DocumentSnapshot updatedDoc = docRef.get().get();
        return updatedDoc.toObject(BudgetCalculator.class);
    }

    @Override
    public String deleteBudgetCalculation(String calculatorId) 
            throws ExecutionException, InterruptedException {
        Firestore dbFirestore = FirestoreClient.getFirestore();
        DocumentReference docRef = dbFirestore.collection(COLLECTION_NAME).document(calculatorId);

        // Check if document exists first
        if (!docRef.get().get().exists()) {
            throw new IllegalArgumentException("Budget calculation not found with ID: " + calculatorId);
        }

        ApiFuture<WriteResult> writeResult = docRef.delete();
        writeResult.get();
        return "Budget calculation with ID " + calculatorId + " deleted successfully";
    }

    @Override
    public BudgetCalculator calculateTemporaryBudget(BudgetCalculator calculator) {
        // Perform calculation without saving to database
        calculator.setTotalBudget(calculateTotal(calculator));
        return calculator;
    }

    // Helper method to calculate total budget
    private double calculateTotal(BudgetCalculator calculator) {
        return (calculator.getDailyAllowance() * calculator.getTripDurationDays()) + 
               calculator.getAccommodationCost() + 
               calculator.getTransportCost() + 
               calculator.getActivityCosts();
    }
}