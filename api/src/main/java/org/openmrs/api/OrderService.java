/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.api;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.db.OrderDAO;
import org.openmrs.util.PrivilegeConstants;

/**
 * Contains methods pertaining to creating/deleting/voiding Orders
 */
public interface OrderService extends OpenmrsService {
	
	/**
	 * Setter for the Order data access object. The dao is used for saving and getting orders
	 * to/from the database
	 * 
	 * @param dao The data access object to use
	 */
	public void setOrderDAO(OrderDAO dao);
	
	/**
	 * Save or update the given <code>order</code> in the database. If the OrderType for the order
	 * is not specified, then it will be set to the one associated to the ConceptClass of the
	 * ordered concept, if none is found then it will default to the one set on the passed in
	 * OrderContext if any otherwise the save fails. If the CareSetting field of the order is not
	 * specified then it will default to the one set on the passed in OrderContext if any otherwise
	 * the save fails.
	 * 
	 * @param order the Order to save
	 * @param orderContext the OrderContext object
	 * @return the Order that was saved
	 * @throws APIException
	 * @should not save order if order doesnt validate
	 * @should discontinue existing active order if new order being saved with action to discontinue
	 * @should pass if the existing drug order matches the concept and drug of the DC order
	 * @should fail if the existing drug order matches the concept and not drug of the DC order
	 * @should discontinue previousOrder if it is not already discontinued
	 * @should fail if concept in previous order does not match this concept
	 * @should not allow editing an existing order
	 * @should not allow revising a voided order
	 * @should not allow revising a stopped order
	 * @should not allow revising an expired order
	 * @should not allow revising an order with no previous order
	 * @should save a revised order
	 * @should set order number specified in the context if specified
	 * @should set the order number returned by the configured generator
	 * @should set order type if null but mapped to the concept class
	 * @should fail if order type is null and not mapped to the concept class
	 * @should default to care setting and order type defined in the order context if null
	 */
	@Authorized( { PrivilegeConstants.EDIT_ORDERS, PrivilegeConstants.ADD_ORDERS })
	public Order saveOrder(Order order, OrderContext orderContext) throws APIException;
	
	/**
	 * Completely delete an order from the database. This should not typically be used unless
	 * desperately needed. Most orders should just be voided. See {@link #voidOrder(Order, String)}
	 * 
	 * @param order The Order to remove from the system
	 * @throws APIException
	 */
	@Authorized(PrivilegeConstants.PURGE_ORDERS)
	public void purgeOrder(Order order) throws APIException;
	
	/**
	 * Completely delete an order from the database. This should not typically be used unless
	 * desperately needed. Most orders should just be voided. See {@link #voidOrder(Order, String)}
	 * This method is different from purgeOrder(Order order) above: If param cascade is false will
	 * completely delete an order from the database period If param cascade is true will completely
	 * delete an order from the database and delete any Obs that references the Order.
	 * 
	 * @param order The Order to remove from the system
	 * @param cascade
	 * @throws APIException
	 * @since 1.9.4
	 * @should delete order
	 * @should delete order when cascade is false
	 * @should delete order when cascade is true and also delete any Obs that references it
	 */
	@Authorized(PrivilegeConstants.PURGE_ORDERS)
	public void purgeOrder(Order order, boolean cascade) throws APIException;
	
	/**
	 * Mark an order as voided. This functionally removes the Order from the system while keeping a
	 * semblance
	 * 
	 * @param voidReason String reason
	 * @param order Order to void
	 * @return the Order that was voided
	 * @throws APIException
	 */
	@Authorized(PrivilegeConstants.DELETE_ORDERS)
	public Order voidOrder(Order order, String voidReason) throws APIException;
	
	/**
	 * Get order by internal primary key identifier
	 * 
	 * @param orderId internal order identifier
	 * @return order with given internal identifier
	 * @throws APIException
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDERS)
	public Order getOrder(Integer orderId) throws APIException;
	
	/**
	 * Get Order by its UUID
	 * 
	 * @param uuid
	 * @return
	 * @should find object given valid uuid
	 * @should return null if no object found with given uuid
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDERS)
	public Order getOrderByUuid(String uuid) throws APIException;
	
	/**
	 * Gets all Orders that match the specified parameters excluding discontinuation orders
	 * 
	 * @param patient the patient to match on
	 * @param careSetting the CareSetting to match on
	 * @param orderType The OrderType to match on
	 * @param includeVoided Specifies whether voided orders should be included or not
	 * @return list of Orders matching the parameters
	 * @since 1.10
	 * @should fail if patient is null
	 * @should fail if careSetting is null
	 * @should get the orders that match all the arguments
	 * @should get all unvoided matches if includeVoided is set to false
	 * @should include voided matches if includeVoided is set to true
	 * @should include orders for sub types if order type is specified
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDERS)
	public List<Order> getOrders(Patient patient, CareSetting careSetting, OrderType orderType, boolean includeVoided);
	
	/**
	 * Gets all orders for the specified patient including discontinuation orders
	 * 
	 * @param patient the patient to match on
	 * @return list of matching {@link org.openmrs.Order}
	 * @since 1.10
	 * @should fail if patient is null
	 * @should get all the orders for the specified patient
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDERS)
	public List<Order> getAllOrdersByPatient(Patient patient);
	
	/**
	 * Unvoid order record. Reverse a previous call to {@link #voidOrder(Order, String)}
	 * 
	 * @param order order to be unvoided
	 * @return the Order that was unvoided
	 */
	@Authorized(PrivilegeConstants.DELETE_ORDERS)
	public Order unvoidOrder(Order order) throws APIException;
	
	/**
	 * Gets the order identified by a given order number
	 * 
	 * @param orderNumber the order number
	 * @return the order object
	 * @should find object given valid order number
	 * @should return null if no object found with given order number
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDERS)
	public Order getOrderByOrderNumber(String orderNumber);
	
	/**
	 * Gets all Order objects that use this Concept for a given patient. Orders will be returned in
	 * the order in which they occurred, i.e. sorted by startDate starting with the latest
	 * 
	 * @param patient the patient.
	 * @param concept the concept.
	 * @return the list of orders.
	 * @should return orders with the given concept
	 * @should return empty list for concept without orders
	 * @should reject a null patient
	 * @should reject a null concept
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDERS)
	public List<Order> getOrderHistoryByConcept(Patient patient, Concept concept);
	
	/**
	 * Gets the next available order number seed
	 * 
	 * @return the order number seed
	 */
	@Authorized(PrivilegeConstants.ADD_ORDERS)
	public Long getNextOrderNumberSeedSequenceValue();
	
	/**
	 * Gets the order matching the specified order number and its previous orders in the ordering
	 * they occurred, i.e if this order has a previous order, fetch it and if it also has a previous
	 * order then fetch it until the original one with no previous order is reached
	 * 
	 * @param orderNumber the order number whose history to get
	 * @return a list of orders for given order number
	 * @should return all order history for given order number
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDERS)
	public List<Order> getOrderHistoryByOrderNumber(String orderNumber);
	
	/**
	 * Gets all active orders for the specified patient matching the specified CareSetting,
	 * OrderType as of the specified date. Below is the criteria for determining an active order:
	 * 
	 * <pre>
	 * <p>
	 * - Not voided
	 * - Not a discontinuation Order i.e one where action != Action#DISCONTINUE
	 * - startDate is before or equal to asOfDate
	 * - dateStopped and autoExpireDate are both null OR if it has dateStopped, then it should be
	 * after asOfDate OR if it has autoExpireDate, then it should be after asOfDate. NOTE: If both
	 * dateStopped and autoExpireDate are set then dateStopped wins because an order can never
	 * expire and then stopped later i.e. you stop an order that hasn't yet expired
	 * <p/>
	 * <pre/>
	 * 
	 * @param patient the patient
	 * @param orderType The OrderType to match
	 * @param careSetting the care setting, returns all ignoring care setting if value is null
	 * @param asOfDate defaults to current time
	 * @return all active orders for given patient parameters
	 * @since 1.10
	 * @should return all active orders for the specified patient
	 * @should return all active orders for the specified patient and care setting
	 * @should return all active drug orders for the specified patient
	 * @should return all active test orders for the specified patient
	 * @should fail if patient is null
	 * @should return active orders as of the specified date
	 * @should return all orders if no orderType is specified
	 * @should include orders for sub types if order type is specified
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDERS)
	public List<Order> getActiveOrders(Patient patient, OrderType orderType, CareSetting careSetting, Date asOfDate);
	
	/**
	 * Retrieve care setting
	 * 
	 * @param careSettingId
	 * @return the care setting
	 * @since 1.10
	 */
	@Authorized(PrivilegeConstants.VIEW_CARE_SETTINGS)
	public CareSetting getCareSetting(Integer careSettingId);
	
	/**
	 * Gets the CareSetting with the specified uuid
	 * 
	 * @param uuid the uuid to match on
	 * @return CareSetting
	 * @should return the care setting with the specified uuid
	 */
	@Authorized(PrivilegeConstants.VIEW_CARE_SETTINGS)
	public CareSetting getCareSettingByUuid(String uuid);
	
	/**
	 * Gets the CareSetting with the specified name
	 * 
	 * @param name the name to match on
	 * @return CareSetting
	 * @should return the care setting with the specified name
	 */
	@Authorized(PrivilegeConstants.VIEW_CARE_SETTINGS)
	public CareSetting getCareSettingByName(String name);
	
	/**
	 * Gets all non retired CareSettings if includeRetired is set to true otherwise retired ones are
	 * included too
	 * 
	 * @param includeRetired specifies whether retired care settings should be returned or not
	 * @return A List of CareSettings
	 * @should return only un retired care settings if includeRetired is set to false
	 * @should return retired care settings if includeRetired is set to true
	 */
	@Authorized(PrivilegeConstants.VIEW_CARE_SETTINGS)
	public List<CareSetting> getCareSettings(boolean includeRetired);
	
	/**
	 * Gets OrderType that matches the specified name
	 * 
	 * @param orderTypeName the name to match against
	 * @return OrderType
	 * @since 1.10
	 * @should return the order type that matches the specified name
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDER_TYPES)
	public OrderType getOrderTypeByName(String orderTypeName);
	
	/**
	 * Gets OrderFrequency that matches the specified orderFrequencyId
	 * 
	 * @param orderFrequencyId the id to match against
	 * @return OrderFrequency
	 * @since 1.10
	 * @should return the order frequency that matches the specified id
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDER_FREQUENCIES)
	public OrderFrequency getOrderFrequency(Integer orderFrequencyId);
	
	/**
	 * Gets OrderFrequency that matches the specified uuid
	 * 
	 * @param uuid the uuid to match against
	 * @return OrderFrequency
	 * @since 1.10
	 * @should return the order frequency that matches the specified uuid
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDER_FREQUENCIES)
	public OrderFrequency getOrderFrequencyByUuid(String uuid);
	
	/**
	 * Gets an OrderFrequency that matches the specified concept
	 * 
	 * @param concept the concept to match against
	 * @return OrderFrequency
	 * @since 1.10
	 * @should return the order frequency that matches the specified concept
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDER_FREQUENCIES)
	public OrderFrequency getOrderFrequencyByConcept(Concept concept);
	
	/**
	 * Gets all order frequencies
	 * 
	 * @return List<OrderFrequency>
	 * @since 1.10
	 * @param includeRetired specifies whether retired ones should be included or not
	 * @should return only non retired order frequencies if includeRetired is set to false
	 * @should return all the order frequencies if includeRetired is set to true
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDER_FREQUENCIES)
	public List<OrderFrequency> getOrderFrequencies(boolean includeRetired);
	
	/**
	 * Gets all non retired order frequencies associated to concepts that match the specified search
	 * phrase
	 * 
	 * @param searchPhrase The string to match on
	 * @param locale The locale to match on when searching in associated concept names
	 * @param exactLocale If false then order frequencies associated to concepts with names in a
	 *            broader locale will be matched e.g in case en_GB is passed in then en will be
	 *            matched
	 * @param includeRetired Specifies if retired order frequencies that match should be included or
	 *            not
	 * @return List<OrderFrequency>
	 * @since 1.10
	 * @should get non retired frequencies with names matching the phrase if includeRetired is false
	 * @should include retired frequencies if includeRetired is set to true
	 * @should get frequencies with names that match the phrase and locales if exact locale is false
	 * @should get frequencies with names that match the phrase and locale if exact locale is true
	 * @should return unique frequencies
	 * @should reject a null search phrase
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDER_FREQUENCIES)
	public List<OrderFrequency> getOrderFrequencies(String searchPhrase, Locale locale, boolean exactLocale,
	        boolean includeRetired);
	
	/**
	 * Discontinues an order. Creates a new order that discontinues the orderToDiscontinue
	 * 
	 * @param orderToDiscontinue
	 * @param reasonCoded
	 * @param discontinueDate
	 * @param orderer
	 * @param encounter
	 * @return the new order that discontinued orderToDiscontinue
	 * @throws APIException if the <code>action</code> of orderToDiscontinue is
	 *             <code>Order.Action.DISCONTINUE</code>
	 * @since 1.10
	 * @should populate correct attributes on the discontinue and discontinued orders
	 * @should fail for a discontinue order
	 * @should fail for a stopped order
	 * @should fail for an expired order
	 * @should reject a future discontinueDate
	 * @should fail for a discontinuation order
	 */
	@Authorized(PrivilegeConstants.ADD_ORDERS)
	public Order discontinueOrder(Order orderToDiscontinue, Concept reasonCoded, Date discontinueDate, Provider orderer,
	        Encounter encounter) throws Exception;
	
	/**
	 * Discontinues an order. Creates a new order that discontinues the orderToDiscontinue.
	 * 
	 * @param orderToDiscontinue
	 * @param reasonNonCoded
	 * @param discontinueDate
	 * @param orderer
	 * @param encounter
	 * @return the new order that discontinued orderToDiscontinue
	 * @throws APIException if the <code>action</code> of orderToDiscontinue is
	 *             <code>Order.Action.DISCONTINUE</code>
	 * @since 1.10
	 * @should populate correct attributes on the discontinue and discontinued orders
	 * @should fail for a discontinue order
	 * @should fail if discontinueDate is in the future
	 * @should fail for a voided order
	 */
	@Authorized(PrivilegeConstants.ADD_ORDERS)
	public Order discontinueOrder(Order orderToDiscontinue, String reasonNonCoded, Date discontinueDate, Provider orderer,
	        Encounter encounter) throws Exception;
	
	/**
	 * Creates or updates the given order frequency in the database
	 * 
	 * @param orderFrequency the order frequency to save
	 * @return the order frequency created/saved
	 * @since 1.10
	 * @should add a new order frequency to the database
	 * @should edit an existing order frequency that is not in use
	 * @should not allow editing an existing order frequency that is in use
	 */
	@Authorized(PrivilegeConstants.MANAGE_ORDER_FREQUENCIES)
	public OrderFrequency saveOrderFrequency(OrderFrequency orderFrequency) throws APIException;
	
	/**
	 * Retires the given order frequency in the database
	 * 
	 * @param orderFrequency the order frequency to retire
	 * @param reason the retire reason
	 * @return the retired order frequency
	 * @since 1.10
	 * @should retire given order frequency
	 */
	@Authorized(PrivilegeConstants.MANAGE_ORDER_FREQUENCIES)
	public OrderFrequency retireOrderFrequency(OrderFrequency orderFrequency, String reason);
	
	/**
	 * Restores an order frequency that was previously retired in the database
	 * 
	 * @param orderFrequency the order frequency to unretire
	 * @return the unretired order frequency
	 * @since 1.10
	 * @should unretire given order frequency
	 */
	@Authorized(PrivilegeConstants.MANAGE_ORDER_FREQUENCIES)
	public OrderFrequency unretireOrderFrequency(OrderFrequency orderFrequency);
	
	/**
	 * Completely removes an order frequency from the database
	 * 
	 * @param orderFrequency the order frequency to purge
	 * @since 1.10
	 * @should delete given order frequency
	 * @should not allow deleting an order frequency that is in use
	 */
	@Authorized(PrivilegeConstants.PURGE_ORDER_FREQUENCIES)
	public void purgeOrderFrequency(OrderFrequency orderFrequency) throws APIException;
	
	/**
	 * Get OrderType by orderTypeId
	 * 
	 * @param orderTypeId the orderTypeId to match on
	 * @since 1.10
	 * @return order type object associated with given id
	 * @should find order type object given valid id
	 * @should return null if no order type object found with given id
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDER_TYPES)
	public OrderType getOrderType(Integer orderTypeId);
	
	/**
	 * Get OrderType by uuid
	 * 
	 * @param uuid the uuid to match on
	 * @since 1.10
	 * @return order type object associated with given uuid
	 * @should find order type object given valid uuid
	 * @should return null if no order type object found with given uuid
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDER_TYPES)
	public OrderType getOrderTypeByUuid(String uuid);
	
	/**
	 * Get all order types, if includeRetired is set to true then retired ones will be included
	 * otherwise not
	 * 
	 * @param includeRetired boolean flag which indicate search needs to look at retired order types
	 *            or not
	 * @should get all order types if includeRetired is set to true
	 * @should get all non retired order types if includeRetired is set to false
	 * @return list of order types
	 * @since 1.10
	 */
	@Authorized(PrivilegeConstants.VIEW_ORDER_TYPES)
	public List<OrderType> getOrderTypes(boolean includeRetired);
	
	/**
	 * Creates or updates the given order type in the database
	 * 
	 * @param orderType the order type to save
	 * @return the order type created/saved
	 * @since 1.10
	 * @should add a new order type to the database
	 * @should edit an existing order type
	 */
	@Authorized(PrivilegeConstants.MANAGE_ORDER_TYPES)
	public OrderType saveOrderType(OrderType orderType);
	
	/**
	 * Completely removes an order type from the database
	 * 
	 * @param orderType the order type to purge
	 * @since 1.10
	 * @should delete given order type
	 * @should not allow deleting an order type that is in use
	 */
	@Authorized(PrivilegeConstants.PURGE_ORDER_TYPES)
	public void purgeOrderType(OrderType orderType) throws APIException;
	
	/**
	 * Retires the given order type in the database
	 * 
	 * @param orderType the order type to retire
	 * @param reason the retire reason
	 * @return the retired order type
	 * @since 1.10
	 * @should retire given order type
	 */
	@Authorized(PrivilegeConstants.MANAGE_ORDER_TYPES)
	public OrderType retireOrderType(OrderType orderType, String reason);
	
	/**
	 * Restores an order type that was previously retired in the database
	 * 
	 * @param orderType the order type to unretire
	 * @return the unretired order type
	 * @since 1.10
	 * @should unretire given order type
	 */
	@Authorized(PrivilegeConstants.MANAGE_ORDER_TYPES)
	public OrderType unretireOrderType(OrderType orderType);
	
	/**
	 * Returns all descendants of a given order type for example Given TEST will get back LAB TEST
	 * and RADIOLOGY TEST; and Given LAB TEST, will might get back SEROLOGY, MICROBIOLOGY, and
	 * CHEMISTRY
	 * 
	 * @param orderType the order type which needs to search for its' dependencies
	 * @param includeRetired boolean flag for include retired order types or not
	 * @return list of order type which matches the given order type
	 */
	@Authorized(PrivilegeConstants.MANAGE_ORDER_TYPES)
	public List<OrderType> getSubtypes(OrderType orderType, boolean includeRetired);
}
