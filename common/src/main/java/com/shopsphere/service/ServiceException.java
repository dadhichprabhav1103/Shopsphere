package com.shopsphere.service;

/** Business-rule or data-access failure. The message is safe to show to the user. */
public class ServiceException extends Exception {
    private static final long serialVersionUID = 1L;

    public ServiceException(String message) { super(message); }
    public ServiceException(String message, Throwable cause) { super(message, cause); }
}
