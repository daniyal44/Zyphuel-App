package com.example.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.data.OrderEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Official Order Invoice Generation & PDF Download Engine for Zyphuel.
 * Enables both customers and riders to generate, preview, save as PDF, and share
 * official itemized receipts to eliminate billing disputes.
 */
object InvoiceGenerator {

    /**
     * Formats an itemized text receipt suitable for SMS, WhatsApp, and clipboard sharing.
     */
    fun generatePlainTextReceipt(order: OrderEntity): String {
        val deliveryFee = FeeConstants.calculateDeliveryFee(order.serviceType)
        val subtotal = (order.totalPrice - deliveryFee).coerceAtLeast(0.0)
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(order.createdAt))

        return buildString {
            appendLine("═════════════════════════════════════════")
            appendLine("       ⚡ ZYPHUEL ON-DEMAND DELIVERY ⚡")
            appendLine("      Official Order Receipt & Tax Invoice")
            appendLine("═════════════════════════════════════════")
            appendLine("🧾 Invoice No:   INV-ZYP-${order.id}")
            appendLine("🆔 Order ID:     #${order.id}")
            appendLine("📅 Date & Time:  $dateStr")
            appendLine("📊 Status:       ${order.status.uppercase(Locale.US)}")
            appendLine("💳 Payment:      ${order.paymentMethod}")
            appendLine("─────────────────────────────────────────")
            appendLine("👤 CUSTOMER DETAILS:")
            appendLine("• Name:    ${order.customerName}")
            appendLine("• Phone:   ${order.customerPhone}")
            appendLine("• Address: ${order.deliveryAddress}")
            appendLine("─────────────────────────────────────────")
            appendLine("🚚 DISPATCH & RIDER:")
            appendLine("• Driver:  ${order.riderName ?: "Zyphuel Central Dispatch"}")
            appendLine("• Contact: ${order.riderEmail ?: "dispatch@zyphuel.com"}")
            appendLine("─────────────────────────────────────────")
            appendLine("📋 ITEM SUMMARY:")
            appendLine("• Item:     ${order.serviceType}")
            appendLine("• Quantity: ${order.quantity} units")
            appendLine("• Subtotal: Rs. ${String.format(Locale.US, "%,.2f", subtotal)}")
            appendLine("• Delivery: Rs. ${String.format(Locale.US, "%,.2f", deliveryFee)}")
            appendLine("═════════════════════════════════════════")
            appendLine("💰 TOTAL COD PAYABLE: Rs. ${String.format(Locale.US, "%,.2f", order.totalPrice)}")
            appendLine("═════════════════════════════════════════")
            appendLine("📞 UAN / Support: +92 323 0112464")
            appendLine("🌐 Lahore, Punjab, Pakistan • www.zyphuel.com")
            appendLine("Thank you for choosing Zyphuel!")
        }
    }

    /**
     * Generates a complete, responsive HTML invoice document for PDF rendering.
     */
    fun generateHtmlInvoice(order: OrderEntity): String {
        val deliveryFee = FeeConstants.calculateDeliveryFee(order.serviceType)
        val subtotal = (order.totalPrice - deliveryFee).coerceAtLeast(0.0)
        val dateStr = SimpleDateFormat("dd MMMM yyyy - hh:mm a", Locale.US).format(Date(order.createdAt))

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>Invoice #${order.id} - Zyphuel</title>
            <style>
                body {
                    font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                    margin: 0;
                    padding: 24px;
                    color: #1e293b;
                    background-color: #ffffff;
                    font-size: 13px;
                    line-height: 1.5;
                }
                .invoice-box {
                    max-width: 680px;
                    margin: auto;
                    border: 1px solid #e2e8f0;
                    border-radius: 12px;
                    padding: 28px;
                    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
                }
                .header-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 24px;
                }
                .brand-title {
                    font-size: 26px;
                    font-weight: 800;
                    color: #0284c7;
                    letter-spacing: -0.5px;
                    margin: 0;
                }
                .brand-tagline {
                    font-size: 11px;
                    color: #64748b;
                    margin: 2px 0 0 0;
                    font-weight: 600;
                    text-transform: uppercase;
                }
                .invoice-badge {
                    text-align: right;
                }
                .invoice-title {
                    font-size: 20px;
                    font-weight: 700;
                    color: #0f172a;
                    margin: 0;
                }
                .invoice-meta {
                    font-size: 12px;
                    color: #64748b;
                    margin-top: 4px;
                }
                .divider {
                    height: 2px;
                    background: linear-gradient(to right, #0284c7, #38bdf8);
                    margin: 16px 0 20px 0;
                    border: none;
                }
                .info-grid {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 24px;
                }
                .info-col {
                    width: 50%;
                    vertical-align: top;
                    padding: 8px 12px;
                    background: #f8fafc;
                    border-radius: 8px;
                }
                .info-label {
                    font-size: 10px;
                    font-weight: 700;
                    color: #0284c7;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                    margin-bottom: 4px;
                }
                .info-value {
                    font-size: 13px;
                    color: #1e293b;
                    font-weight: 600;
                }
                .info-sub {
                    font-size: 12px;
                    color: #64748b;
                }
                .items-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-bottom: 24px;
                }
                .items-table th {
                    background: #0f172a;
                    color: #ffffff;
                    text-align: left;
                    padding: 10px 14px;
                    font-size: 11px;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                .items-table td {
                    padding: 12px 14px;
                    border-bottom: 1px solid #f1f5f9;
                }
                .summary-table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .summary-table td {
                    padding: 6px 14px;
                }
                .summary-label {
                    text-align: right;
                    color: #64748b;
                    font-weight: 500;
                    width: 70%;
                }
                .summary-val {
                    text-align: right;
                    color: #1e293b;
                    font-weight: 600;
                    width: 30%;
                }
                .total-row td {
                    border-top: 2px solid #0f172a;
                    padding-top: 10px;
                }
                .total-label {
                    text-align: right;
                    font-size: 15px;
                    font-weight: 800;
                    color: #0f172a;
                }
                .total-val {
                    text-align: right;
                    font-size: 18px;
                    font-weight: 800;
                    color: #0284c7;
                }
                .status-chip {
                    display: inline-block;
                    padding: 3px 10px;
                    border-radius: 9999px;
                    font-size: 11px;
                    font-weight: 700;
                    background: #e0f2fe;
                    color: #0369a1;
                    text-transform: uppercase;
                }
                .footer {
                    margin-top: 28px;
                    padding-top: 16px;
                    border-top: 1px dashed #cbd5e1;
                    text-align: center;
                    color: #94a3b8;
                    font-size: 11px;
                }
                .security-stamp {
                    display: inline-block;
                    margin-top: 10px;
                    padding: 4px 12px;
                    border: 1px solid #10b981;
                    color: #059669;
                    border-radius: 4px;
                    font-weight: 700;
                    font-size: 10px;
                    text-transform: uppercase;
                }
            </style>
        </head>
        <body>
            <div class="invoice-box">
                <table class="header-table">
                    <tr>
                        <td>
                            <div class="brand-title">⚡ ZYPHUEL</div>
                            <div class="brand-tagline">On-Demand Doorstep Fuel & Clean Water</div>
                            <div style="font-size: 11px; color: #64748b; margin-top: 4px;">
                                Central Operations: Green Town HQ / Model Town, Lahore<br/>
                                Helpline: +92 323 0112464 • NTN: 8941203-7
                            </div>
                        </td>
                        <td class="invoice-badge">
                            <div class="invoice-title">TAX INVOICE</div>
                            <div class="invoice-meta">
                                <b>INV-ZYP-${order.id}</b><br/>
                                Order #${order.id}<br/>
                                $dateStr
                            </div>
                            <div style="margin-top: 6px;">
                                <span class="status-chip">${order.status}</span>
                            </div>
                        </td>
                    </tr>
                </table>

                <hr class="divider" />

                <table class="info-grid">
                    <tr>
                        <td class="info-col">
                            <div class="info-label">Customer Details</div>
                            <div class="info-value">${order.customerName}</div>
                            <div class="info-sub">📞 ${order.customerPhone}</div>
                            <div class="info-sub">📧 ${order.customerEmail}</div>
                            <div class="info-sub" style="margin-top: 4px;">📍 <b>Delivery Address:</b> ${order.deliveryAddress}</div>
                        </td>
                        <td style="width: 2%;"></td>
                        <td class="info-col">
                            <div class="info-label">Fulfillment & Rider Details</div>
                            <div class="info-value">${order.riderName ?: "Zyphuel Bowser Direct Dispatch"}</div>
                            <div class="info-sub">📧 ${order.riderEmail ?: "dispatch@zyphuel.com"}</div>
                            <div class="info-sub" style="margin-top: 4px;">💳 <b>Payment Mode:</b> ${order.paymentMethod}</div>
                            <div class="info-sub">⏱️ <b>Estimated Delivery:</b> ${if (order.etaMinutes > 0) "${order.etaMinutes} Mins" else "On Schedule"}</div>
                        </td>
                    </tr>
                </table>

                <table class="items-table">
                    <thead>
                        <tr>
                            <th>Item Description</th>
                            <th style="text-align: center;">Qty</th>
                            <th style="text-align: right;">Amount (PKR)</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>
                                <b>${order.serviceType}</b>
                                <div style="font-size: 11px; color: #64748b;">OGRA Regulated Direct Doorstep Delivery</div>
                            </td>
                            <td style="text-align: center; font-weight: 600;">${order.quantity} Units</td>
                            <td style="text-align: right; font-weight: 600;">Rs. ${String.format(Locale.US, "%,.2f", subtotal)}</td>
                        </tr>
                    </tbody>
                </table>

                <table class="summary-table">
                    <tr>
                        <td class="summary-label">Items Subtotal:</td>
                        <td class="summary-val">Rs. ${String.format(Locale.US, "%,.2f", subtotal)}</td>
                    </tr>
                    <tr>
                        <td class="summary-label">Doorstep Logistics & Handling:</td>
                        <td class="summary-val">Rs. ${String.format(Locale.US, "%,.2f", deliveryFee)}</td>
                    </tr>
                    <tr>
                        <td class="summary-label">Government Taxes (Included):</td>
                        <td class="summary-val" style="color: #10b981;">Rs. 0.00</td>
                    </tr>
                    <tr class="total-row">
                        <td class="total-label">Total COD Payable:</td>
                        <td class="total-val">Rs. ${String.format(Locale.US, "%,.2f", order.totalPrice)}</td>
                    </tr>
                </table>

                <div class="footer">
                    <div>This is a computer-generated tax invoice verified by the Zyphuel Delivery Protocol.</div>
                    <div class="security-stamp">✓ Verified Transaction • Official Order Document</div>
                    <div style="margin-top: 6px;">Questions regarding this invoice? Contact support at support@zyphuel.com or +92 323 0112464.</div>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * Triggers the native Android PrintManager to either Print or "Save as PDF" to device storage.
     */
    fun printOrSavePdf(context: Context, order: OrderEntity) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "Printing service unavailable on this device", Toast.LENGTH_SHORT).show()
                return
            }

            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val jobName = "Zyphuel_Invoice_${order.id}"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("pdf_res", "High Quality", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()

                    printManager.print(jobName, printAdapter, attributes)
                }
            }

            webView.loadDataWithBaseURL(null, generateHtmlInvoice(order), "text/html", "UTF-8", null)
            Toast.makeText(context, "Opening PDF Download / Print dialog...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch PDF print: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shares the formatted invoice text via native Android sharing sheet (WhatsApp, Email, etc.).
     */
    fun shareInvoice(context: Context, order: OrderEntity) {
        try {
            val receiptText = generatePlainTextReceipt(order)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Zyphuel Order Invoice #${order.id}")
                putExtra(Intent.EXTRA_TEXT, receiptText)
            }
            context.startActivity(Intent.createChooser(intent, "Share Zyphuel Order Invoice"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share invoice: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
