package org.sigmah.client.mvp;

import org.sigmah.client.dispatch.AsyncMonitor;
import org.sigmah.shared.dto.DTO;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

public interface CanDelete<M extends DTO> {
	public interface ConfirmDeleteHandler extends EventHandler {
		void onConfirmDelete(ConfirmDeleteEvent deleteEvent);
	}
	public interface RequestDeleteHandler extends EventHandler {
		void onRequestDelete(RequestDeleteEvent deleteEvent);
	}
	public interface CancelDeleteHandler extends EventHandler {
		void onCancelDelete(CancelDeleteEvent cancelDeleteEvent);
	}

	// The user intends to remove an item
	public HandlerRegistration addRequestDeleteHandler(RequestDeleteHandler handler);
	
	// The user confirmed his request to remove an item
	public HandlerRegistration addConfirmDeleteHandler(ConfirmDeleteHandler handler);

	// The user confirmed his request to remove an item
	public HandlerRegistration addCancelDeleteHandler(CancelDeleteHandler handler);

	// Update the views' store with deletion information
	public void delete(M item);
	
	// The presenter wants to know from the user if he really intends to remove the item
	public void askConfirmDelete(M item);
	
	// The user wants to exit the delete entity mode
	public void cancelDelete();
	
	// Whether or not the user should be asked to confirm deletion, or remove the entity right away
	public void setMustConfirmDelete(boolean mustConfirmDelete);
	
	// If true, the delete button is enabled
	public void setDeleteEnabled(boolean deleteEnabled);
	
	// Let the user know what's going on during deleting
	public AsyncMonitor getDeletingMonitor();
	
	// Since View<T> extends TakesValue<T>, the value does not need to be encapsulated
	public class RequestDeleteEvent extends GwtEvent<RequestDeleteHandler> {
		public static Type TYPE = new Type<RequestDeleteHandler>(); 
		
		@Override
		public Type<RequestDeleteHandler> getAssociatedType() {
			return TYPE;
		}

		@Override
		protected void dispatch(RequestDeleteHandler handler) {
			handler.onRequestDelete(this);
		}
	}
	
	// Since View<T> extends TakesValue<T>, the value does not need to be encapsulated
	public class ConfirmDeleteEvent extends GwtEvent<ConfirmDeleteHandler> {
		public static Type TYPE = new Type<ConfirmDeleteHandler>(); 
		
		@Override
		public Type<ConfirmDeleteHandler> getAssociatedType() {
			return TYPE;
		}

		@Override
		protected void dispatch(ConfirmDeleteHandler handler) {
			handler.onConfirmDelete(this);
		}
	}
	
	// Since View<T> extends TakesValue<T>, the value does not need to be encapsulated
	public class CancelDeleteEvent extends GwtEvent<CancelDeleteHandler> {
		public static Type TYPE = new Type<CancelDeleteHandler>(); 
		
		@Override
		public Type<CancelDeleteHandler> getAssociatedType() {
			return TYPE;
		}

		@Override
		protected void dispatch(CancelDeleteHandler handler) {
			handler.onCancelDelete(this);
		}
	}
}
