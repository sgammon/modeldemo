/* This file was generated by the ServiceGenerator.
 * The ServiceGenerator is Copyright (c) 2016 Google Inc.
 */

//
//  GTLQueryUnifiedAPI.h
//

// ----------------------------------------------------------------------------
// NOTE: This file is generated from Google APIs Discovery Service.
// Service:
//   Unified API (unified/v1)
// Description:
//   Sample combined API.
// Classes:
//   GTLQueryUnifiedAPI (5 custom class methods, 5 custom properties)

#if GTL_BUILT_AS_FRAMEWORK
  #import "GTL/GTLQuery.h"
#else
  #import "GTLQuery.h"
#endif

@interface GTLQueryUnifiedAPI : GTLQuery

//
// Parameters valid on all methods.
//

// Selector specifying which fields to include in a partial response.
@property (nonatomic, copy) NSString *fields;

//
// Method-specific parameters; see the comments below for more information.
//
@property (nonatomic, copy) NSString *key;
@property (nonatomic, copy) NSString *message;
@property (nonatomic, copy) NSString *name;
@property (nonatomic, copy) NSString *options;

#pragma mark - Service level methods
// These create a GTLQueryUnifiedAPI object.

// Method: unified.create
//  Authorization scope(s):
//   kGTLAuthScopeUnifiedAPIUserinfoEmail
// Fetches a GTLUnifiedAPISerializedKey.
+ (instancetype)queryForCreateWithMessage:(NSString *)message
                                     name:(NSString *)name;

// Method: unified.delete
//  Authorization scope(s):
//   kGTLAuthScopeUnifiedAPIUserinfoEmail
+ (instancetype)queryForDeleteWithKey:(NSString *)key;

// Method: unified.errors
//  Authorization scope(s):
//   kGTLAuthScopeUnifiedAPIUserinfoEmail
// Fetches a GTLUnifiedAPIErrorSpec.
+ (instancetype)queryForErrors;

// Method: unified.list
//  Optional:
//   options: NSString
//  Authorization scope(s):
//   kGTLAuthScopeUnifiedAPIUserinfoEmail
// Fetches a GTLUnifiedAPISerializedQueryResponse.
+ (instancetype)queryForList;

// Method: unified.update
//  Authorization scope(s):
//   kGTLAuthScopeUnifiedAPIUserinfoEmail
+ (instancetype)queryForUpdateWithKey:(NSString *)key
                              message:(NSString *)message;

@end
