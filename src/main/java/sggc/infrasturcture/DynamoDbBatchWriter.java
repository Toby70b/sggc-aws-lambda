package sggc.infrasturcture;

import com.google.common.collect.Iterables;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

import java.util.List;
import java.util.Set;

/**
 * Represents a class which is used to perform batch writes an Amazon DynamoDB instance
 */
//TODO rename to DynamoDBBatchWriter and remove all static method, take an enhanced client as a field
public class DynamoDbBatchWriter {

    //AWS limits the amount of items in a batch to 25
    private static final int DYNAMODB_MAX_BATCH_SIZE = 25;

    /**
     * Submits a list of Items to be written in batch to a DynamoDb table
     *
     * @param itemType the type of the object to write
     * @param items    the list of objects to write the size of the list should be no greater than 25 items due to
     *                 limitations with DynamoDB
     * @param client   an instance of the DynamoDb enhanced client, used to write to a DynamoDB table in batch
     * @param table    the DynamoDB table to write to
     * @param <T>      the type of object that will be written to the DynamoDB table
     */
    public <T> void batchWrite(Class<T> itemType, Set<T> items, DynamoDbEnhancedClient client, DynamoDbTable<T> table) {
        Iterable<List<T>> partitions = Iterables.partition(items, DYNAMODB_MAX_BATCH_SIZE);
        partitions.forEach(batchOfItems -> {
            List<T> unprocessedItems = submitBatchWrite(itemType, batchOfItems, client, table);
            while (!unprocessedItems.isEmpty()) {
                // Catch any items that failed to be persisted due to provisioning issues
                unprocessedItems = submitBatchWrite(itemType, unprocessedItems, client, table);
            }
        });
    }

    /**
     * Method used to batch write a number of items to a DynamoDB table using an instance of the DynamoDB enhanced client
     *
     * @param itemType the type of the object to write
     * @param items    the list of objects to write the size of the list should be no greater than 25 items due to
     *                 limitations with DynamoDB
     * @param client   an instance of the DynamoDb enhanced client, used to write to a DynamoDB table in batch
     * @param table    the DynamoDB table to write to
     * @param <T>      the type of object that will be written to the DynamoDB table
     * @return a list of objects of type T that couldn't be written to the DynamoDB table due to provisioning issues
     */
    private <T> List<T> submitBatchWrite(Class<T> itemType, List<T> items, DynamoDbEnhancedClient client, DynamoDbTable<T> table) {
        WriteBatch.Builder<T> subBatchBuilder = WriteBatch.builder(itemType).mappedTableResource(table);
        items.forEach(subBatchBuilder::addPutItem);
        BatchWriteItemEnhancedRequest.Builder overallBatchBuilder = BatchWriteItemEnhancedRequest.builder();
        overallBatchBuilder.addWriteBatch(subBatchBuilder.build());
        return client.batchWriteItem(overallBatchBuilder.build()).unprocessedPutItemsForTable(table);
    }
}
